package com.movtery.zalithlauncher.game.launch

import android.app.Activity
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.unit.IntSize
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ZLApplication
import com.movtery.zalithlauncher.bridge.LoggerBridge.append
import com.movtery.zalithlauncher.bridge.LoggerBridge.appendTitle
import com.movtery.zalithlauncher.bridge.ZLBridge
import com.movtery.zalithlauncher.context.readAssetFile
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountType
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.multirt.Runtime
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.path.getLibrariesHome
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.plugin.renderer.RendererPluginManager
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.support.touch_controller.ControllerProxy
import com.movtery.zalithlauncher.game.version.download.artifactToPath
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.getGameManifest
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.file.child
import com.movtery.zalithlauncher.utils.file.ensureDirectorySilently
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext

class GameLauncher(
    private val activity: Activity,
    private val version: Version,
    private val getWindowSize: () -> IntSize,
    onExit: (code: Int, isSignal: Boolean) -> Unit
) : Launcher(onExit) {
    private lateinit var gameManifest: GameManifest

    override suspend fun launch(): Int {
        if (!Renderers.isCurrentRendererValid()) {
            Renderers.setCurrentRenderer(activity, version.getRenderer())
        }

        gameManifest = getGameManifest(version)
        CallbackBridge.nativeSetUseInputStackQueue(gameManifest.arguments != null)

        val currentAccount = AccountsManager.currentAccountFlow.value!!
        val account = if (version.offlineAccountLogin) {
            //使用临时离线账号启动游戏
            Account(
                username = currentAccount.username,
                accountType = AccountType.LOCAL.tag
            )
        } else {
            currentAccount
        }
        val customArgs = version.getJvmArgs().takeIf { it.isNotBlank() } ?: AllSettings.jvmArgs.getValue()
        val javaRuntime = getRuntime()

        printLauncherInfo(
            javaArguments = customArgs.takeIf { it.isNotEmpty() } ?: "NONE",
            javaRuntime = javaRuntime,
            account = account
        )

        return launchGame(
            account = account,
            javaRuntime = javaRuntime,
            customArgs = customArgs
        )
    }

    override fun chdir(): String {
        return version.getGameDir().absolutePath
    }

    override fun getLogName(): String = "latest_game"

    override fun initEnv(jreHome: String, runtime: Runtime): MutableMap<String, String> {
        val envMap = super.initEnv(jreHome, runtime)

        DriverPluginManager.setDriverById(version.getDriver())
        envMap["DRIVER_PATH"] = DriverPluginManager.getDriver().path

        checkAndUsedJSPH(envMap, runtime)
        version.getVersionInfo()?.loaderInfo?.getLoaderEnvKey()?.let { loaderKey ->
            envMap[loaderKey] = "1"
        }
        if (Renderers.isCurrentRendererValid()) {
            setRendererEnv(envMap)
        }
        envMap["ZALITH_VERSION_CODE"] = BuildConfig.VERSION_CODE.toString()
        return envMap
    }

    override fun dlopenEngine() {
        super.dlopenEngine()
        appendTitle("DLOPEN Renderer")

        //声音引擎加载后，dlopen渲染器的库
        RendererPluginManager.selectedRendererPlugin?.let { renderer ->
            renderer.dlopen.forEach { lib -> ZLBridge.dlopen("${renderer.path}/$lib") }
        }

        val rendererLib = loadGraphicsLibrary() ?: return
        if (!ZLBridge.dlopen(rendererLib) && !ZLBridge.dlopen(findInLdLibPath(rendererLib))) {
            Log.e("GameLauncher", "Failed to load renderer $rendererLib")
        }
    }

    override fun progressFinalUserArgs(args: MutableList<String>, ramAllocation: Int) {
        super.progressFinalUserArgs(args, version.getRamAllocation(activity))
        if (Renderers.isCurrentRendererValid()) {
            args.add("-Dorg.lwjgl.opengl.libname=${loadGraphicsLibrary()}")
        }
    }

    private suspend fun launchGame(
        account: Account,
        javaRuntime: String,
        customArgs: String
    ): Int {
        val runtime = RuntimesManager.forceReload(javaRuntime)

        val gameDirPath = version.getGameDir()

        disableSplash(gameDirPath)
        val launchClassPath = generateLaunchClassPath(gameManifest)

        val launchArgs = LaunchArgs(
            account = account,
            gameDirPath = gameDirPath,
            version = version,
            gameManifest = gameManifest,
            runtime = runtime,
            launchClassPath = launchClassPath,
            readAssetsFile = { path -> activity.readAssetFile(path) },
            getCacioJavaArgs = { isJava8 ->
                val size = getWindowSize()
                getCacioJavaArgs(size.width, size.height, isJava8)
            }
        ).getAllArgs()

        tryStartTouchProxy()

        return launchJvm(
            context = activity,
            jvmArgs = launchArgs,
            userArgs = customArgs,
            getWindowSize = getWindowSize,
            runtime = runtime
        )
    }

    private fun tryStartTouchProxy() {
        if (version.isTouchProxyEnabled()) {
            ControllerProxy.startProxy(activity, version.getTouchVibrateDuration())
        }
    }

    private fun printLauncherInfo(
        javaArguments: String,
        javaRuntime: String,
        account: Account
    ) {
        var mcInfo = version.getVersionName()
        version.getVersionInfo()?.let { info -> mcInfo = info.getInfoString() }
        val renderer = Renderers.getCurrentRenderer()

        appendTitle("Launch Minecraft")
        append("Info: Launcher version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        append("Info: Architecture: ${Architecture.archAsString(ZLApplication.DEVICE_ARCHITECTURE)}")
        append("Info: Device model: ${Build.MANUFACTURER}, ${Build.MODEL}")
        append("Info: API version: ${Build.VERSION.SDK_INT}")
        append("Info: Renderer: ${renderer.getRendererName()}")
        renderer.getRendererSummary()?.let { summary ->
            append("Info: Renderer Summary: $summary")
        }
        append("Info: Selected Minecraft version: ${version.getVersionName()}")
        append("Info: Minecraft Info: $mcInfo")
        append("Info: Game Path: ${version.getGameDir().absolutePath} (Isolation: ${version.isIsolation()})")
        append("Info: Custom Java arguments: $javaArguments")
        append("Info: Java Runtime: $javaRuntime")
        append("Info: Account: ${account.username} (${account.accountType})")
    }

    private fun getRuntime(): String {
        val versionRuntime = version.getJavaRuntime().takeIf { it.isNotEmpty() } ?: ""

        if (versionRuntime.isNotEmpty()) return versionRuntime

        val targetJavaVersion = gameManifest.javaVersion?.majorVersion ?: 8

        var runtime = AllSettings.javaRuntime.getValue()
        val pickedRuntime = RuntimesManager.loadRuntime(runtime)

        if (AllSettings.autoPickJavaRuntime.getValue() &&
            (pickedRuntime.javaVersion == 0 || pickedRuntime.javaVersion < targetJavaVersion)) {
            runtime = RuntimesManager.getNearestJreName(targetJavaVersion) ?: run {
                activity.runOnUiThread {
                    Toast.makeText(activity, activity.getString(R.string.game_auto_pick_runtime_failed), Toast.LENGTH_SHORT).show()
                }
                return runtime
            }
        }
        return runtime
    }

    /**
     * 禁用Forge的启动屏幕
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/a6f3fc0/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java#L372-L391)
     */
    private fun disableSplash(dir: File) {
        File(dir, "config").let { configDir ->
            if (configDir.ensureDirectorySilently()) {
                val forgeSplashFile = configDir.child("splash.properties")
                runCatching {
                    var forgeSplashContent = "enabled=true"
                    if (forgeSplashFile.exists()) {
                        forgeSplashContent = forgeSplashFile.readText()
                    }
                    if (forgeSplashContent.contains("enabled=true")) {
                        forgeSplashFile.writeText(
                            forgeSplashContent.replace("enabled=true", "enabled=false")
                        )
                    }
                }.onFailure {
                    Log.w("GameLauncher", "Could not disable Forge 1.12.2 and below splash screen!", it)
                }
            } else {
                Log.w("GameLauncher", "Failed to create the configuration directory")
            }
        }
    }

    /**
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/a6f3fc0/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java#L572-L592)
     */
    private fun generateLaunchClassPath(
        gameManifest: GameManifest,
        isClientFirst: Boolean = false
    ): String {
        val classpathList = mutableListOf<String>()

        val classpath: Array<String> = generateLibClasspath(gameManifest)

        val clientClass = File(version.getVersionPath(), "${version.getVersionName()}.jar")
        val clientClasspath: String = clientClass.absolutePath

        if (isClientFirst && clientClass.exists()) {
            classpathList.add(clientClasspath)
        }
        for (jarFile in classpath) {
            val jarFileObj = File(jarFile)
            if (!jarFileObj.exists()) {
                Log.d("GameLauncher", "Ignored non-exists file: $jarFile")
                continue
            }
            classpathList.add(jarFile)
        }
        if (!isClientFirst && clientClass.exists()) {
            classpathList.add(clientClasspath)
        }

        return classpathList.joinToString(":")
    }

    /**
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/a6f3fc0/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java#L871-L882)
     */
    private fun generateLibClasspath(gameManifest: GameManifest): Array<String> {
        val libDir: MutableList<String> = ArrayList()
        for (libItem in gameManifest.libraries) {
            if (!checkRules(libItem.rules)) continue
            val libArtifactPath: String = artifactToPath(libItem) ?: continue
            libDir.add(getLibrariesHome() + "/" + libArtifactPath)
        }
        return libDir.toTypedArray<String>()
    }

    /**
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/a6f3fc0/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java#L815-L823)
     */
    private fun checkRules(rules: List<GameManifest.Rule>?): Boolean {
        if (rules == null) return true // always allow

        for (rule in rules) {
            if (rule.action.equals("allow") && rule.os != null && rule.os.name.equals("osx")) {
                return false //disallow
            }
        }
        return true // allow if none match
    }

    companion object {
        private fun checkAndUsedJSPH(envMap: MutableMap<String, String>, runtime: Runtime) {
            if (runtime.javaVersion < 11) return //onUseJSPH
            val dir = File(PathManager.DIR_NATIVE_LIB).takeIf { it.isDirectory } ?: return
            val jsphHome = if (runtime.javaVersion == 17) "libjsph17" else "libjsph21"
            dir.listFiles { _, name -> name.startsWith(jsphHome) }?.takeIf { it.isNotEmpty() }?.let {
                val libName = "${PathManager.DIR_NATIVE_LIB}/$jsphHome.so"
                envMap["JSP"] = libName
            }
        }

        private fun setRendererEnv(envMap: MutableMap<String, String>) {
            val renderer = Renderers.getCurrentRenderer()
            val rendererId = renderer.getRendererId()

            if (rendererId.startsWith("opengles3")) {
                envMap["LIBGL_ES"] = "3"
                envMap["LIBGL_GLES"] = "libGLESv3.so"
                envMap["LIBGL_MIPMAP"] = "3"
                envMap["LIBGL_NOERROR"] = "1"
                envMap["LIBGL_NOINTOVLHACK"] = "1"
                envMap["LIBGL_NORMALIZE"] = "1"
            }

            envMap += renderer.getRendererEnv().value

            renderer.getRendererEGL()?.let { eglName ->
                envMap["POJAVEXEC_EGL"] = eglName
            }

            envMap["POJAV_RENDERER"] = rendererId
            envMap["TAG_RENDERER"] = rendererId
            if (RendererPluginManager.selectedRendererPlugin != null) return

            if (!rendererId.startsWith("opengles")) {
                envMap["MESA_LOADER_DRIVER_OVERRIDE"] = "zink"
                envMap["MESA_GLSL_CACHE_DIR"] = PathManager.DIR_CACHE.absolutePath
                envMap["force_glsl_extensions_warn"] = "true"
                envMap["allow_higher_compat_version"] = "true"
                envMap["allow_glsl_extension_directive_midshader"] = "true"
                envMap["LIB_MESA_NAME"] = loadGraphicsLibrary() ?: "null"
            }

            if (!envMap.containsKey("LIBGL_ES")) {
                val glesMajor = getDetectedVersion()
                Log.i("glesDetect", "GLES version detected: $glesMajor")

                envMap["LIBGL_ES"] = if (glesMajor < 3) {
                    //fallback to 2 since it's the minimum for the entire app
                    "2"
                } else if (rendererId.startsWith("opengles")) {
                    rendererId.replace("opengles", "").replace("_5", "")
                } else {
                    // TODO if can: other backends such as Vulkan.
                    // Sure, they should provide GLES 3 support.
                    "3"
                }
            }
        }

        /**
         * Open the render library in accordance to the settings.
         * It will fallback if it fails to load the library.
         * @return The name of the loaded library
         */
        private fun loadGraphicsLibrary(): String? {
            if (!Renderers.isCurrentRendererValid()) return null
            else {
                val rendererPlugin = RendererPluginManager.selectedRendererPlugin
                return if (rendererPlugin != null) {
                    "${rendererPlugin.path}/${rendererPlugin.glName}"
                } else {
                    Renderers.getCurrentRenderer().getRendererLibrary()
                }
            }
        }

        /**
         * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/98947f2/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/utils/JREUtils.java#L505-L516)
         */
        private fun hasExtension(extensions: String, name: String): Boolean {
            var start = extensions.indexOf(name)
            while (start >= 0) {
                // check that we didn't find a prefix of a longer extension name
                val end = start + name.length
                if (end == extensions.length || extensions[end] == ' ') {
                    return true
                }
                start = extensions.indexOf(name, end)
            }
            return false
        }

        private const val EGL_OPENGL_ES_BIT: Int = 0x0001
        private const val EGL_OPENGL_ES2_BIT: Int = 0x0004
        private const val EGL_OPENGL_ES3_BIT_KHR: Int = 0x0040

        fun getDetectedVersion(): Int {
            val egl = EGLContext.getEGL() as EGL10
            val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
            val numConfigs = IntArray(1)
            if (egl.eglInitialize(display, null)) {
                try {
                    val checkES3: Boolean = hasExtension(egl.eglQueryString(display, EGL10.EGL_EXTENSIONS), "EGL_KHR_create_context")
                    if (egl.eglGetConfigs(display, null, 0, numConfigs)) {
                        val configs = arrayOfNulls<EGLConfig>(
                            numConfigs[0]
                        )
                        if (egl.eglGetConfigs(display, configs, numConfigs[0], numConfigs)) {
                            var highestEsVersion = 0
                            val value = IntArray(1)
                            for (i in 0..<numConfigs[0]) {
                                if (egl.eglGetConfigAttrib(
                                        display, configs[i],
                                        EGL10.EGL_RENDERABLE_TYPE, value
                                    )
                                ) {
                                    if (checkES3 && ((value[0] and EGL_OPENGL_ES3_BIT_KHR) == EGL_OPENGL_ES3_BIT_KHR)) {
                                        if (highestEsVersion < 3) highestEsVersion = 3
                                    } else if ((value[0] and EGL_OPENGL_ES2_BIT) == EGL_OPENGL_ES2_BIT) {
                                        if (highestEsVersion < 2) highestEsVersion = 2
                                    } else if ((value[0] and EGL_OPENGL_ES_BIT) == EGL_OPENGL_ES_BIT) {
                                        if (highestEsVersion < 1) highestEsVersion = 1
                                    }
                                } else {
                                    Log.w(
                                        "glesDetect", ("Getting config attribute with "
                                                + "EGL10#eglGetConfigAttrib failed "
                                                + "(" + i + "/" + numConfigs[0] + "): "
                                                + egl.eglGetError())
                                    )
                                }
                            }
                            return highestEsVersion
                        } else {
                            Log.e(
                                "glesDetect", "Getting configs with EGL10#eglGetConfigs failed: "
                                        + egl.eglGetError()
                            )
                            return -1
                        }
                    } else {
                        Log.e(
                            "glesDetect",
                            "Getting number of configs with EGL10#eglGetConfigs failed: "
                                    + egl.eglGetError()
                        )
                        return -2
                    }
                } finally {
                    egl.eglTerminate(display)
                }
            } else {
                Log.e("glesDetect", "Couldn't initialize EGL.")
                return -3
            }
        }
    }
}

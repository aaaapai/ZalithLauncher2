package com.movtery.zalithlauncher.game.launch

import android.content.Context
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.util.ArrayMap
import androidx.annotation.CallSuper
import androidx.compose.ui.unit.IntSize
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.bridge.ZLBridge
import com.movtery.zalithlauncher.bridge.ZLNativeInvoker
import com.movtery.zalithlauncher.game.multirt.Runtime
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.path.GamePathManager
import com.movtery.zalithlauncher.game.path.getGameHome
import com.movtery.zalithlauncher.game.plugin.ffmpeg.FFmpegPluginManager
import com.movtery.zalithlauncher.game.plugin.renderer.RendererPluginManager
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.path.LibPath
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.scaleFactor
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.device.Architecture.ARCH_X86
import com.movtery.zalithlauncher.utils.device.Architecture.is64BitsDevice
import com.movtery.zalithlauncher.utils.file.child
import com.movtery.zalithlauncher.utils.getDisplayFriendlyRes
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.oracle.dalvik.VMLauncher
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import java.util.TimeZone

abstract class Launcher(
    val onExit: (code: Int, isSignal: Boolean) -> Unit
) {
    lateinit var runtime: Runtime
        protected set

    private val runtimeHome: String by lazy {
        RuntimesManager.getRuntimeHome(runtime.name).absolutePath
    }

    var libraryPath: String = ""
        private set

    private var dirNameHomeJre: String = "lib"
    private var jvmLibraryPath: String = ""

    abstract suspend fun launch(): Int
    abstract fun chdir(): String
    abstract fun getLogName(): String

    protected suspend fun launchJvm(
        context: Context,
        jvmArgs: List<String>,
        userHome: String? = null,
        userArgs: String,
        getWindowSize: () -> IntSize
    ): Int {
        ZLNativeInvoker.staticLauncher = this

        initLdLibraryPath(runtimeHome)

        LoggerBridge.appendTitle("Env Map")
        setEnv(runtimeHome, runtime)

        LoggerBridge.appendTitle("DLOPEN Java Runtime")
        dlopenJavaRuntime(runtimeHome)

        dlopenEngine()

        return launchJavaVM(
            context = context,
            jvmArgs = jvmArgs,
            runtimeHome = runtimeHome,
            userHome = userHome,
            userArgs = userArgs,
            getWindowSize = getWindowSize
        )
    }

    //伪 suspend 函数，等待 JVM 的退出代码
    private suspend fun launchJavaVM(
        context: Context,
        jvmArgs: List<String>,
        runtimeHome: String,
        userHome: String? = null,
        userArgs: String,
        getWindowSize: () -> IntSize
    ): Int {
        val args = getJavaArgs(userHome, userArgs, getWindowSize, runtimeHome).toMutableList()
        progressFinalUserArgs(args)

        args.addAll(jvmArgs)

        LoggerBridge.appendTitle("JVM Args")
        val iterator = args.iterator()
        while (iterator.hasNext()) {
            val arg = iterator.next()
            if (arg.startsWith("--accessToken") && iterator.hasNext()) {
                iterator.next()
                LoggerBridge.append("JVMArgs: $arg")
                LoggerBridge.append("JVMArgs: ********************")
                continue
            }
            LoggerBridge.append("JVMArgs: $arg")
        }

        ZLBridge.setupExitMethod(context.applicationContext)
        ZLBridge.initializeGameExitHook()
        ZLBridge.chdir(chdir())

        args.add(0, "java") //argv[0] is the program name according to C standard.

        val exitCode = VMLauncher.launchJVM(args.toTypedArray())
        LoggerBridge.append("Java Exit code: $exitCode")
        return exitCode
    }

    /**
     * @param args 需要进行处理的参数
     * @param ramAllocation 指定内存空间大小
     */
    protected open fun progressFinalUserArgs(
        args: MutableList<String>,
        ramAllocation: Int = AllSettings.ramAllocation.getValue()
    ) {
        args.purgeArg("-Xms")
        args.purgeArg("-Xmx")
        args.purgeArg("-d32")
        args.purgeArg("-d64")
        args.purgeArg("-Xint")
        args.purgeArg("-XX:+UseTransparentHugePages")
        args.purgeArg("-XX:+UseLargePagesInMetaspace")
        args.purgeArg("-XX:+UseLargePages")
        args.purgeArg("-Dorg.lwjgl.opengl.libname")
        // Don't let the user specify a custom Freetype library (as the user is unlikely to specify a version compiled for Android)
        args.purgeArg("-Dorg.lwjgl.freetype.libname")
        // Overridden by us to specify the exact number of cores that the android system has
        args.purgeArg("-XX:ActiveProcessorCount")

        args.add("-javaagent:${LibPath.MIO_LIB_PATCHER.absolutePath}")

        //Add automatically generated args
        val ramAllocationString = ramAllocation.toString()
        args.add("-Xms${ramAllocationString}M")
        args.add("-Xmx${ramAllocationString}M")

        // Force LWJGL to use the Freetype library intended for it, instead of using the one
        // that we ship with Java (since it may be older than what's needed)
        args.add("-Dorg.lwjgl.freetype.libname=${PathManager.DIR_NATIVE_LIB}/libfreetype.so")

        // Some phones are not using the right number of cores, fix that
        args.add("-XX:ActiveProcessorCount=${java.lang.Runtime.getRuntime().availableProcessors()}")
    }

    protected fun MutableList<String>.purgeArg(argStart: String) {
        removeIf { arg: String -> arg.startsWith(argStart) }
    }

    protected fun relocateLibPath() {
        var jreArchitecture = runtime.arch
        if (Architecture.archAsInt(jreArchitecture) == ARCH_X86) {
            jreArchitecture = "i386/i486/i586"
        }

        for (arch in jreArchitecture.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val f = File(runtimeHome, "lib/$arch")
            if (f.exists() && f.isDirectory) {
                dirNameHomeJre = "lib/$arch"
            }
        }

        val libName = if (is64BitsDevice) "lib64" else "lib"
        val path = listOfNotNull(
            FFmpegPluginManager.takeIf { it.isAvailable }?.libraryPath,
            RendererPluginManager.selectedRendererPlugin?.path,
            "$runtimeHome/$dirNameHomeJre/jli",
            "$runtimeHome/$dirNameHomeJre",
            "/system/$libName",
            "/vendor/$libName",
            "/vendor/$libName/hw",
            LibPath.JNA.absolutePath,
            PathManager.DIR_NATIVE_LIB
        )
        this.libraryPath = path.joinToString(":")
    }

    private fun initLdLibraryPath(jreHome: String) {
        val serverFile = File(jreHome).child(dirNameHomeJre, "server", "libjvm.so")
        jvmLibraryPath = "$jreHome/$dirNameHomeJre/" + (if (serverFile.exists()) "server" else "client")
        lDebug("Base libraryPath: $libraryPath")
        lDebug("Internal libraryPath: $jvmLibraryPath:$libraryPath")
        ZLBridge.setLdLibraryPath("$jvmLibraryPath:$libraryPath")
    }

    protected fun findInLdLibPath(libName: String): String {
        val path = Os.getenv("LD_LIBRARY_PATH") ?: run {
            try {
                if (libraryPath.isNotEmpty()) {
                    Os.setenv("LD_LIBRARY_PATH", libraryPath, true)
                }
            } catch (e: ErrnoException) {
                lError("Failed to locate lib path", e)
            }
            libraryPath
        }
        return path.split(":").find { libPath ->
            val file = File(libPath, libName)
            file.exists() && file.isFile
        }?.let {
            File(it, libName).absolutePath
        } ?: libName
    }

    private fun locateLibs(path: File): List<File> {
        val children = path.listFiles() ?: return emptyList()
        return children.flatMap { file ->
            when {
                file.isFile && file.name.endsWith(".so") -> listOf(file)
                file.isDirectory -> locateLibs(file)
                else -> emptyList()
            }
        }
    }

    private fun setEnv(jreHome: String, runtime: Runtime) {
        val envMap = initEnv(jreHome, runtime)
        envMap.forEach { (key, value) ->
            LoggerBridge.append("Added env: $key = $value")
            runCatching {
                Os.setenv(key, value, true)
            }.onFailure {
                lError("Unable to set environment variable.", it)
            }
        }
    }

    @CallSuper
    protected open fun initEnv(jreHome: String, runtime: Runtime): MutableMap<String, String> {
        val envMap: MutableMap<String, String> = ArrayMap()
        setJavaEnv(envMap = { envMap }, jreHome = jreHome)
        return envMap
    }

    private fun setJavaEnv(envMap: () -> MutableMap<String, String>, jreHome: String) {
        envMap().let { map ->
            map["POJAV_NATIVEDIR"] = PathManager.DIR_NATIVE_LIB
            map["JAVA_HOME"] = jreHome
            map["HOME"] = PathManager.DIR_FILES_EXTERNAL.absolutePath
            map["TMPDIR"] = PathManager.DIR_CACHE.absolutePath
            map["LD_LIBRARY_PATH"] = libraryPath
            map["PATH"] = "$jreHome/bin:${Os.getenv("PATH")}"
            map["AWTSTUB_WIDTH"] = (CallbackBridge.windowWidth.takeIf { it > 0 } ?: CallbackBridge.physicalWidth).toString()
            map["AWTSTUB_HEIGHT"] = (CallbackBridge.windowHeight.takeIf { it > 0 } ?: CallbackBridge.physicalHeight).toString()

            if (AllSettings.dumpShaders.getValue()) map["LIBGL_VGPU_DUMP"] = "1"
            if (AllSettings.zinkPreferSystemDriver.getValue()) map["POJAV_ZINK_PREFER_SYSTEM_DRIVER"] = "1"
            if (AllSettings.vsyncInZink.getValue()) map["POJAV_VSYNC_IN_ZINK"] = "1"
            if (AllSettings.bigCoreAffinity.getValue()) map["POJAV_BIG_CORE_AFFINITY"] = "1"

            if (FFmpegPluginManager.isAvailable) map["POJAV_FFMPEG_PATH"] = FFmpegPluginManager.executablePath!!
        }
    }

    private fun dlopenJavaRuntime(jreHome: String) {
        ZLBridge.dlopen(findInLdLibPath("libjli.so"))
        if (!ZLBridge.dlopen("libjvm.so")) {
            lWarning("Failed to load with no path, trying with full path")
            ZLBridge.dlopen("$jvmLibraryPath/libjvm.so")
        }
        ZLBridge.dlopen(findInLdLibPath("libverify.so"))
        ZLBridge.dlopen(findInLdLibPath("libjava.so"))
        ZLBridge.dlopen(findInLdLibPath("libnet.so"))
        ZLBridge.dlopen(findInLdLibPath("libnio.so"))
        ZLBridge.dlopen(findInLdLibPath("libawt.so"))
        ZLBridge.dlopen(findInLdLibPath("libawt_headless.so"))
        ZLBridge.dlopen(findInLdLibPath("libfreetype.so"))
        ZLBridge.dlopen(findInLdLibPath("libfontmanager.so"))
        locateLibs(File(jreHome, dirNameHomeJre)).forEach { file ->
            ZLBridge.dlopen(file.absolutePath)
        }
    }

    @CallSuper
    protected open fun dlopenEngine() {
        ZLBridge.dlopen("${PathManager.DIR_NATIVE_LIB}/libopenal.so")
    }

    companion object {

        /**
         * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/98947f2/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/utils/JREUtils.java#L345-L401)
         */
        fun getJavaArgs(
            userHome: String? = null,
            userArgumentsString: String,
            getWindowSize: () -> IntSize,
            runtimeHome: String
        ): List<String> {
            val userArguments = parseJavaArguments(userArgumentsString).toMutableList()
            val resolvFile = File(PathManager.DIR_FILES_PRIVATE.parent, "resolv.conf").absolutePath

            val windowSize = getWindowSize()

            val overridableArguments = mutableMapOf<String, String>().apply {
                put("java.home", runtimeHome)
                put("java.io.tmpdir", PathManager.DIR_CACHE.absolutePath)
                put("jna.boot.library.path", PathManager.DIR_NATIVE_LIB)
                put("user.home", userHome ?: GamePathManager.getUserHome())
                System.getProperty("user.language")?.let { put("user.language", it) }
                put("os.name", "Linux")
                put("os.version", "Android-${Build.VERSION.RELEASE}")
                put("pojav.path.minecraft", getGameHome())
                put("pojav.path.private.account", PathManager.DIR_DATA_BASES.absolutePath)
                put("user.timezone", TimeZone.getDefault().id)
                put("org.lwjgl.vulkan.libname", "libvulkan.so")
                put("glfwstub.windowWidth", getDisplayFriendlyRes(windowSize.width, scaleFactor).toString())
                put("glfwstub.windowHeight", getDisplayFriendlyRes(windowSize.height, scaleFactor).toString())
                put("glfwstub.initEgl", "false")
                put("ext.net.resolvPath", resolvFile)

                put("log4j2.formatMsgNoLookups", "true")
                // Fix RCE vulnerability of log4j2
                put("java.rmi.server.useCodebaseOnly", "true")
                put("com.sun.jndi.rmi.object.trustURLCodebase", "false")
                put("com.sun.jndi.cosnaming.object.trustURLCodebase", "false")

                put("net.minecraft.clientmodname", InfoDistributor.LAUNCHER_NAME)

                // fml
                put("fml.earlyprogresswindow", "false")
                put("fml.ignoreInvalidMinecraftCertificates", "true")
                put("fml.ignorePatchDiscrepancies", "true")

                put("loader.disable_forked_guis", "true")
                put("jdk.lang.Process.launchMechanism", "FORK")

                put("sodium.checks.issue2561", "false")
            }.map { entry ->
                "-D${entry.key}=${entry.value}"
            }

            val additionalArguments = overridableArguments.filter { arg ->
                val stripped = arg.substringBefore('=')
                val overridden = userArguments.any { it.startsWith(stripped) }
                if (overridden) {
                    lInfo("Arg skipped: $arg")
                }
                !overridden
            }

            userArguments += additionalArguments
            return userArguments
        }

        /**
         * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/98947f2/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/utils/JREUtils.java#L411-L456)
         */
        fun parseJavaArguments(args: String): List<String> {
            val parsedArguments = mutableListOf<String>()
            var cleanedArgs = args.trim().replace(" ", "")
            val separators = listOf("-XX:-", "-XX:+", "-XX:", "--", "-D", "-X", "-javaagent:", "-verbose")

            for (prefix in separators) {
                while (true) {
                    val start = cleanedArgs.indexOf(prefix)
                    if (start == -1) break

                    val end = separators
                        .mapNotNull { sep ->
                            val i = cleanedArgs.indexOf(sep, start + prefix.length)
                            if (i != -1) i else null
                        }
                        .minOrNull() ?: cleanedArgs.length

                    val parsedSubstring = cleanedArgs.substring(start, end)
                    cleanedArgs = cleanedArgs.replace(parsedSubstring, "")

                    if (parsedSubstring.indexOf('=') == parsedSubstring.lastIndexOf('=')) {
                        val last = parsedArguments.lastOrNull()
                        if (last != null && (last.endsWith(',') || parsedSubstring.contains(','))) {
                            parsedArguments[parsedArguments.lastIndex] = last + parsedSubstring
                        } else {
                            parsedArguments.add(parsedSubstring)
                        }
                    } else {
                        lWarning("Removed improper arguments: $parsedSubstring")
                    }
                }
            }

            return parsedArguments
        }

        fun getCacioJavaArgs(
            screenWidth: Int,
            screenHeight: Int,
            isJava8: Boolean
        ): List<String> {
            val argsList: MutableList<String> = ArrayList()

            // Caciocavallo config AWT-enabled version
            argsList.add("-Djava.awt.headless=false")
            argsList.add("-Dcacio.managed.screensize=" + (screenWidth * 0.8).toInt() + "x" + (screenHeight * 0.8).toInt())
            argsList.add("-Dcacio.font.fontmanager=sun.awt.X11FontManager")
            argsList.add("-Dcacio.font.fontscaler=sun.font.FreetypeFontScaler")
            argsList.add("-Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel")
            if (isJava8) {
                argsList.add("-Dawt.toolkit=net.java.openjdk.cacio.ctc.CTCToolkit")
                argsList.add("-Djava.awt.graphicsenv=net.java.openjdk.cacio.ctc.CTCGraphicsEnvironment")
            } else {
                argsList.add("-Dawt.toolkit=com.github.caciocavallosilano.cacio.ctc.CTCToolkit")
                argsList.add("-Djava.awt.graphicsenv=com.github.caciocavallosilano.cacio.ctc.CTCGraphicsEnvironment")
                argsList.add("-Djava.system.class.loader=com.github.caciocavallosilano.cacio.ctc.CTCPreloadClassLoader")
                argsList.add("--add-exports=java.desktop/java.awt=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/java.awt.peer=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt.image=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/java.awt.dnd.peer=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt.event=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.awt.datatransfer=ALL-UNNAMED")
                argsList.add("--add-exports=java.desktop/sun.font=ALL-UNNAMED")
                argsList.add("--add-exports=java.base/sun.security.action=ALL-UNNAMED")
                argsList.add("--add-opens=java.base/java.util=ALL-UNNAMED")
                argsList.add("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
                argsList.add("--add-opens=java.desktop/sun.font=ALL-UNNAMED")
                argsList.add("--add-opens=java.desktop/sun.java2d=ALL-UNNAMED")
                argsList.add("--add-opens=java.base/java.lang.reflect=ALL-UNNAMED")

                // Opens the java.net package to Arc DNS injector on Java 9+
                argsList.add("--add-opens=java.base/java.net=ALL-UNNAMED")
            }

            val cacioClassPath = StringBuilder()
            cacioClassPath.append("-Xbootclasspath/").append(if (isJava8) "p" else "a")
            val cacioFiles = if (isJava8) LibPath.CACIO_8 else LibPath.CACIO_17
            cacioFiles.listFiles()?.onEach {
                if (it.name.endsWith(".jar")) cacioClassPath.append(":").append(it.absolutePath)
            }

            argsList.add(cacioClassPath.toString())

            return argsList
        }
    }
}

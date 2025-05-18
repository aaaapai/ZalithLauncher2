package com.movtery.zalithlauncher.game.launch

import android.content.Context
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.util.ArrayMap
import android.util.Log
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
import com.movtery.zalithlauncher.utils.string.StringUtils
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.getMessageOrToString
import com.oracle.dalvik.VMLauncher
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import java.util.TimeZone

abstract class Launcher(
    val onExit: (code: Int, isSignal: Boolean) -> Unit
) {
    private var dirNameHomeJre: String = "lib"
    private var ldLibraryPath: String = ""
    private var jvmLibraryPath: String = ""

    abstract suspend fun launch(): Int
    abstract fun chdir(): String
    abstract fun getLogName(): String

    protected suspend fun launchJvm(
        context: Context,
        jvmArgs: List<String>,
        userHome: String? = null,
        userArgs: String,
        getWindowSize: () -> IntSize,
        runtime: Runtime
    ): Int {
        ZLNativeInvoker.staticLauncher = this

        val runtimeHome = RuntimesManager.getRuntimeHome(runtime.name).absolutePath
        relocateLibPath(runtime, runtimeHome)
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

    private fun relocateLibPath(runtime: Runtime, jreHome: String) {
        var jreArchitecture = runtime.arch
        if (Architecture.archAsInt(jreArchitecture) == ARCH_X86) {
            jreArchitecture = "i386/i486/i586"
        }

        for (arch in jreArchitecture.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val f = File(jreHome, "lib/$arch")
            if (f.exists() && f.isDirectory) {
                dirNameHomeJre = "lib/$arch"
            }
        }

        val libName = if (is64BitsDevice) "lib64" else "lib"
        val ldLibraryPath = java.lang.StringBuilder()
        if (FFmpegPluginManager.isAvailable) {
            ldLibraryPath.append(FFmpegPluginManager.libraryPath!!).append(":")
        }
        val customRenderer = RendererPluginManager.selectedRendererPlugin
        if (customRenderer != null) {
            ldLibraryPath.append(customRenderer.path).append(":")
        }
        ldLibraryPath.append(jreHome)
            .append("/").append(dirNameHomeJre)
            .append("/jli:").append(jreHome).append("/").append(dirNameHomeJre)
            .append(":")
        ldLibraryPath.append("/system/").append(libName).append(":")
            .append("/vendor/").append(libName).append(":")
            .append("/vendor/").append(libName).append("/hw:")
            .append(PathManager.DIR_NATIVE_LIB)
        this.ldLibraryPath = ldLibraryPath.toString()
    }

    private fun initLdLibraryPath(jreHome: String) {
        val serverFile = File(jreHome).child(dirNameHomeJre, "server", "libjvm.so")
        jvmLibraryPath = "$jreHome/$dirNameHomeJre/" + (if (serverFile.exists()) "server" else "client")
        Log.d("DynamicLoader", "Base ldLibraryPath: $ldLibraryPath")
        Log.d("DynamicLoader", "Internal ldLibraryPath: $jvmLibraryPath:$ldLibraryPath")
        ZLBridge.setLdLibraryPath("$jvmLibraryPath:$ldLibraryPath")
    }

    protected fun findInLdLibPath(libName: String): String {
        val path = Os.getenv("LD_LIBRARY_PATH") ?: run {
            try {
                if (ldLibraryPath.isNotEmpty()) {
                    Os.setenv("LD_LIBRARY_PATH", ldLibraryPath, true)
                }
            } catch (e: ErrnoException) {
                Log.e("Launcher", StringUtils.throwableToString(e))
            }
            ldLibraryPath
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
                Log.e("Launcher", it.getMessageOrToString())
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
            map["LD_LIBRARY_PATH"] = ldLibraryPath
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
            Log.w("DynamicLoader", "Failed to load with no path, trying with full path")
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

            val overridableArguments = listOf(
                "-Djava.home=$runtimeHome",
                "-Djava.io.tmpdir=${PathManager.DIR_CACHE.absolutePath}",
                "-Djna.boot.library.path=${PathManager.DIR_NATIVE_LIB}",
                "-Duser.home=${userHome ?: GamePathManager.getUserHome()}",
                "-Duser.language=${System.getProperty("user.language")}",
                "-Dos.name=Linux",
                "-Dos.version=Android-${Build.VERSION.RELEASE}",
                "-Dpojav.path.minecraft=${getGameHome()}",
                "-Dpojav.path.private.account=${PathManager.DIR_ACCOUNT}",
                "-Duser.timezone=${TimeZone.getDefault().id}",
                "-Dorg.lwjgl.vulkan.libname=libvulkan.so",
                "-Dglfwstub.windowWidth=${getDisplayFriendlyRes(windowSize.width, scaleFactor)}",
                "-Dglfwstub.windowHeight=${getDisplayFriendlyRes(windowSize.height, scaleFactor)}",
                "-Dglfwstub.initEgl=false",
                "-Dext.net.resolvPath=$resolvFile",
                "-Dlog4j2.formatMsgNoLookups=true",
                "-Dnet.minecraft.clientmodname=${InfoDistributor.LAUNCHER_NAME}",
                "-Dfml.earlyprogresswindow=false",
                "-Dloader.disable_forked_guis=true",
                "-Djdk.lang.Process.launchMechanism=FORK",
                "-Dsodium.checks.issue2561=false"
            )

            val additionalArguments = overridableArguments.filter { arg ->
                val stripped = arg.substringBefore('=')
                val overridden = userArguments.any { it.startsWith(stripped) }
                if (overridden) {
                    Log.i("ArgProcessor", "Arg skipped: $arg")
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
                        Log.w("JAVA ARGS PARSER", "Removed improper arguments: $parsedSubstring")
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

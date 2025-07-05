package com.movtery.zalithlauncher.game.launch

import androidx.collection.ArrayMap
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.isAuthServerAccount
import com.movtery.zalithlauncher.game.multirt.Runtime
import com.movtery.zalithlauncher.game.path.getAssetsHome
import com.movtery.zalithlauncher.game.path.getLibrariesHome
import com.movtery.zalithlauncher.game.version.download.artifactToPath
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.getGameManifest
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.path.LibPath
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.file.child
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.network.ServerAddress
import com.movtery.zalithlauncher.utils.string.StringUtils
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.isNotEmptyOrBlank
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.toUnicodeEscaped
import com.movtery.zalithlauncher.utils.string.isLowerTo
import java.io.File

class LaunchArgs(
    private val launcher: Launcher,
    private val account: Account,
    private val gameDirPath: File,
    private val version: Version,
    private val gameManifest: GameManifest,
    private val runtime: Runtime,
    private val readAssetsFile: (path: String) -> String,
    private val getCacioJavaArgs: (isJava8: Boolean) -> List<String>
) {
    fun getAllArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        argsList.addAll(getJavaArgs())
        argsList.addAll(getMinecraftJVMArgs())

        if (runtime.javaVersion > 8) {
            argsList.add("--add-exports")
            val pkg: String = gameManifest.mainClass.substring(0, gameManifest.mainClass.lastIndexOf("."))
            argsList.add("$pkg/$pkg=ALL-UNNAMED")
        }

        argsList.add(gameManifest.mainClass)
        argsList.addAll(getMinecraftClientArgs())

        version.getVersionInfo()?.let { info ->
            val playSingle = version.quickPlaySingle?.takeIf { it.isNotEmptyOrBlank() }
            if (playSingle != null) { //快速启动单人游戏
                if (info.quickPlay.isQuickPlaySingleplayer) {
                    //将不受支持的字符转换为Unicode
                    val saveName = playSingle.toUnicodeEscaped()
                    argsList.apply {
                        add("--quickPlaySingleplayer")
                        add(saveName)
                    }
                } else {
                    lWarning("Quick Play for singleplayer is not supported and has been skipped.")
                }
            } else {
                version.getServerIp()?.let { address ->
                    val parsed = ServerAddress.parse(address)
                    argsList += if (info.quickPlay.isQuickPlayMultiplayer) {
                        listOf(
                            "--quickPlayMultiplayer",
                            if (parsed.port < 0) "$address:25565" else address
                        )
                    } else {
                        val port = parsed.port.takeIf { it >= 0 } ?: 25565
                        listOf("--server", parsed.host, "--port", port.toString())
                    }
                }
            }
        }

        return argsList
    }

    private fun getLWJGL3ClassPath(): String =
        File(PathManager.DIR_COMPONENTS, "lwjgl3")
            .listFiles { file -> file.name.endsWith(".jar") }
            ?.joinToString(":") { it.absolutePath }
            ?: ""

    private fun getJavaArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        if (account.isAuthServerAccount()) {
            if (account.otherBaseUrl!!.contains("auth.mc-user.com")) {
                argsList.add("-javaagent:${LibPath.NIDE_8_AUTH.absolutePath}=${account.otherBaseUrl!!.replace("https://auth.mc-user.com:233/", "")}")
                argsList.add("-Dnide8auth.client=true")
            } else {
                argsList.add("-javaagent:${LibPath.AUTHLIB_INJECTOR.absolutePath}=${account.otherBaseUrl}")
            }
        }

        argsList.addAll(getCacioJavaArgs(runtime.javaVersion == 8))

        val configFilePath = version.getVersionPath().child("log4j2.xml")
        if (!configFilePath.exists()) {
            val is7 = (gameManifest.id ?: "0.0").isLowerTo("1.12")
            runCatching {
                val content = if (is7) {
                    readAssetsFile("components/log4j-1.7.xml")
                } else {
                    readAssetsFile("components/log4j-1.12.xml")
                }
                configFilePath.writeText(content)
            }.onFailure {
                lWarning("Failed to write fallback Log4j configuration autonomously!", it)
            }
        }
        argsList.add("-Dlog4j.configurationFile=${configFilePath.absolutePath}")
        argsList.add("-Dminecraft.client.jar=${version.getClientJar().absolutePath}")

        val versionSpecificNativesDir = File(PathManager.DIR_CACHE, "natives/${version.getVersionName()}")
        if (versionSpecificNativesDir.exists()) {
            val dirPath = versionSpecificNativesDir.absolutePath
            argsList.add("-Djava.library.path=$dirPath:${PathManager.DIR_NATIVE_LIB}")
            argsList.add("-Djna.boot.library.path=$dirPath")
        }

        return argsList
    }

    private fun getMinecraftJVMArgs(): Array<String> {
        val gameManifest1 = getGameManifest(version, true)

//        // Parse Forge 1.17+ additional JVM Arguments
//        if (versionInfo.inheritsFrom == null || versionInfo.arguments == null || versionInfo.arguments.jvm == null) {
//            return emptyArray()
//        }

        val varArgMap: MutableMap<String, String> = android.util.ArrayMap()
        val launchClassPath = "${getLWJGL3ClassPath()}:${generateLaunchClassPath(gameManifest)}"
        var hasClasspath = false //是否已经在jvm参数中包含 ${classpath} 配置

        varArgMap["classpath_separator"] = ":"
        varArgMap["library_directory"] = getLibrariesHome()
        varArgMap["version_name"] = gameManifest1.id
        varArgMap["natives_directory"] = launcher.libraryPath
        setLauncherInfo(varArgMap)

        fun Any.processJvmArg(): String? = (this as? String)?.let {
            when {
                it.startsWith("-DignoreList=") -> {
                    "$it,${version.getVersionName()}.jar"
                }
                it.contains("-Dio.netty.native.workdir") ||
                it.contains("-Djna.tmpdir") ||
                it.contains("-Dorg.lwjgl.system.SharedLibraryExtractPath") -> {
                    //使用一个可读的目录
                    it.replace("\${natives_directory}", PathManager.DIR_CACHE.absolutePath)
                }
                it == "\${classpath}" -> {
                    hasClasspath = true
                    launchClassPath
                }
                else -> it
            }
        }

        val jvmArgs = gameManifest1.arguments?.jvm
            ?.mapNotNull { it.processJvmArg() }
            ?.toTypedArray()
            ?: emptyArray()

        val replacedArgs = StringUtils.insertJSONValueList(jvmArgs, varArgMap)
        return if (hasClasspath) {
            replacedArgs
        } else {
            //不包含 ${classpath} 配置，则需要手动添加
            replacedArgs + arrayOf("-cp", launchClassPath)
        }
    }

    /**
     * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/a6f3fc0/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Tools.java#L572-L592)
     */
    private fun generateLaunchClassPath(gameManifest: GameManifest): String {
        val classpathList = mutableListOf<String>()

        val classpath: Array<String> = generateLibClasspath(gameManifest)

        val clientClass = version.getClientJar()
        val clientClasspath: String = clientClass.absolutePath

        for (jarFile in classpath) {
            val jarFileObj = File(jarFile)
            if (!jarFileObj.exists()) {
                lDebug("Ignored non-exists file: $jarFile")
                continue
            }
            classpathList.add(jarFile)
        }
        if (clientClass.exists()) {
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

    private fun getMinecraftClientArgs(): Array<String> {
        val varArgMap: MutableMap<String, String> = ArrayMap()
        varArgMap["auth_session"] = account.accessToken
        varArgMap["auth_access_token"] = account.accessToken
        varArgMap["auth_player_name"] = account.username
        varArgMap["auth_uuid"] = account.profileId.replace("-", "")
        varArgMap["auth_xuid"] = account.xUid ?: ""
        varArgMap["assets_root"] = getAssetsHome()
        varArgMap["assets_index_name"] = gameManifest.assets
        varArgMap["game_assets"] = getAssetsHome()
        varArgMap["game_directory"] = gameDirPath.absolutePath
        varArgMap["user_properties"] = "{}"
        varArgMap["user_type"] = "msa"
        varArgMap["version_name"] = version.getVersionInfo()!!.minecraftVersion

        setLauncherInfo(varArgMap)

        val minecraftArgs: MutableList<String> = ArrayList()
        gameManifest.arguments?.apply {
            // Support Minecraft 1.13+
            game.forEach { if (it is String) minecraftArgs.add(it) }
        }

        return StringUtils.insertJSONValueList(
            splitAndFilterEmpty(
                gameManifest.minecraftArguments ?:
                minecraftArgs.toTypedArray().joinToString(" ")
            ), varArgMap
        )
    }

    private fun setLauncherInfo(verArgMap: MutableMap<String, String>) {
        verArgMap["launcher_name"] = InfoDistributor.LAUNCHER_NAME
        verArgMap["launcher_version"] = BuildConfig.VERSION_NAME
        verArgMap["version_type"] = version.getCustomInfo()
            .takeIf { it.isNotEmptyOrBlank() }
            ?: gameManifest.type
    }

    private fun splitAndFilterEmpty(arg: String): Array<String> {
        val list: MutableList<String> = ArrayList()
        arg.split(" ").forEach {
            if (it.isNotEmpty()) list.add(it)
        }
        return list.toTypedArray()
    }
}
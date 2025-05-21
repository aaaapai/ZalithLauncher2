package com.movtery.zalithlauncher.game.launch

import android.util.Log
import androidx.collection.ArrayMap
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.isOtherLoginAccount
import com.movtery.zalithlauncher.game.multirt.Runtime
import com.movtery.zalithlauncher.game.path.getAssetsHome
import com.movtery.zalithlauncher.game.path.getLibrariesHome
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.getGameManifest
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.path.LibPath
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.file.child
import com.movtery.zalithlauncher.utils.string.StringUtils
import com.movtery.zalithlauncher.utils.string.isBiggerOrEqualTo
import com.movtery.zalithlauncher.utils.string.isLowerTo
import java.io.File

class LaunchArgs(
    private val account: Account,
    private val gameDirPath: File,
    private val version: Version,
    private val gameManifest: GameManifest,
    private val runtime: Runtime,
    private val launchClassPath: String,
    private val readAssetsFile: (path: String) -> String,
    private val getCacioJavaArgs: (isJava8: Boolean) -> List<String>
) {
    fun getAllArgs(): List<String> {
        val argsList: MutableList<String> = ArrayList()

        argsList.addAll(getJavaArgs())
        argsList.addAll(getMinecraftJVMArgs())
        argsList.add("-cp")
        argsList.add("${getLWJGL3ClassPath()}:$launchClassPath")

        if (runtime.javaVersion > 8) {
            argsList.add("--add-exports")
            val pkg: String = gameManifest.mainClass.substring(0, gameManifest.mainClass.lastIndexOf("."))
            argsList.add("$pkg/$pkg=ALL-UNNAMED")
        }

        argsList.add(gameManifest.mainClass)
        argsList.addAll(getMinecraftClientArgs())

        version.getServerIp()?.let { serverIp ->
            val (ip, port) = serverIp.split(":").run {
                first() to getOrElse(1) { "25565" }
            }
            version.getVersionInfo()?.minecraftVersion?.let { minecraftVersion ->
                if (
                    minecraftVersion.isBiggerOrEqualTo("23w14a") || //该快照才加入quickPlayMultiplayer参数
                    minecraftVersion.isBiggerOrEqualTo("1.20")      //1.20+
                ) {
                    argsList.apply {
                        add("--quickPlayMultiplayer")
                        add("$ip:$port")
                    }
                } else {
                    argsList.apply {
                        add("--server")
                        add(ip)
                        add("--port")
                        add(port)
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

        if (account.isOtherLoginAccount()) {
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
                Log.w("LaunchArgs", "Failed to write fallback Log4j configuration autonomously!", it)
            }
        }
        argsList.add("-Dlog4j.configurationFile=${configFilePath.absolutePath}")

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
        varArgMap["classpath_separator"] = ":"
        varArgMap["library_directory"] = getLibrariesHome()
        varArgMap["version_name"] = gameManifest1.id
        varArgMap["natives_directory"] = PathManager.DIR_NATIVE_LIB

        val minecraftArgs: MutableList<String> = java.util.ArrayList()
        gameManifest1.arguments?.let {
            fun String.addIgnoreListIfHas(): String {
                if (startsWith("-DignoreList=")) return "$this,${version.getVersionName()}.jar"
                return this
            }
            it.jvm?.forEach { arg ->
                if (arg is String) {
                    minecraftArgs.add(arg.addIgnoreListIfHas())
                }
            }
        }
        return StringUtils.insertJSONValueList(minecraftArgs.toTypedArray<String>(), varArgMap)
    }

    private fun getMinecraftClientArgs(): Array<String> {
        val verArgMap: MutableMap<String, String> = ArrayMap()
        verArgMap["auth_session"] = account.accessToken
        verArgMap["auth_access_token"] = account.accessToken
        verArgMap["auth_player_name"] = account.username
        verArgMap["auth_uuid"] = account.profileId.replace("-", "")
        verArgMap["auth_xuid"] = account.xuid ?: ""
        verArgMap["assets_root"] = getAssetsHome()
        verArgMap["assets_index_name"] = gameManifest.assets
        verArgMap["game_assets"] = getAssetsHome()
        verArgMap["game_directory"] = gameDirPath.absolutePath
        verArgMap["user_properties"] = "{}"
        verArgMap["user_type"] = "msa"
        verArgMap["version_name"] = version.getVersionInfo()!!.minecraftVersion

        setLauncherInfo(verArgMap)

        val minecraftArgs: MutableList<String> = ArrayList()
        gameManifest.arguments?.apply {
            // Support Minecraft 1.13+
            game.forEach { if (it is String) minecraftArgs.add(it) }
        }

        return StringUtils.insertJSONValueList(
            splitAndFilterEmpty(
                gameManifest.minecraftArguments ?:
                minecraftArgs.toTypedArray().joinToString(" ")
            ), verArgMap
        )
    }

    private fun setLauncherInfo(verArgMap: MutableMap<String, String>) {
        verArgMap["launcher_name"] = InfoDistributor.LAUNCHER_NAME
        verArgMap["launcher_version"] = BuildConfig.VERSION_NAME
        verArgMap["version_type"] = version.getCustomInfo()
            .takeIf { it.isNotEmpty() && it.isNotBlank() }
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
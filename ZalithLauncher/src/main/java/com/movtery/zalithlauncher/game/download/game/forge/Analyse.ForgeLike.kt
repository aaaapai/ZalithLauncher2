package com.movtery.zalithlauncher.game.download.game.forge

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.ForgeLikeVersion
import com.movtery.zalithlauncher.game.download.game.GameLibDownloader
import com.movtery.zalithlauncher.game.download.game.copyVanillaFiles
import com.movtery.zalithlauncher.game.download.game.getLibraryPath
import com.movtery.zalithlauncher.game.download.game.models.ForgeLikeInstallProcessor
import com.movtery.zalithlauncher.game.version.download.BaseMinecraftDownloader
import com.movtery.zalithlauncher.game.version.download.artifactToPath
import com.movtery.zalithlauncher.game.version.download.parseTo
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.file.ensureDirectory
import com.movtery.zalithlauncher.utils.file.extractEntryToFile
import com.movtery.zalithlauncher.utils.file.readText
import com.movtery.zalithlauncher.utils.json.merge
import com.movtery.zalithlauncher.utils.json.parseToJson
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import com.movtery.zalithlauncher.utils.network.withRetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

const val FORGE_LIKE_ANALYSE_ID = "Analyse.ForgeLike"

/**
 * Forge Like 分析与安装支持库 (仅支持处理新版本 Forge、NeoForge)
 */
fun getForgeLikeAnalyseTask(
    downloader: BaseMinecraftDownloader,
    targetTempInstaller: File,
    forgeLikeVersion: ForgeLikeVersion,
    tempMinecraftFolder: File,
    sourceInherit: String,
    processedInherit: String,
    loaderVersion: String
): Task {
    return Task.runTask(
        id = FORGE_LIKE_ANALYSE_ID,
        dispatcher = Dispatchers.Default,
        task = { task ->
            if (sourceInherit != processedInherit) {
                withContext(Dispatchers.IO) {
                    //准备安装环境
                    //复制原版文件
                    copyVanillaFiles(
                        sourceGameFolder = tempMinecraftFolder,
                        sourceVersion = sourceInherit,
                        destinationGameFolder = tempMinecraftFolder,
                        targetVersion = processedInherit
                    )
                }
            }

            analyseNewForge(
                task = task,
                downloader = downloader,
                forgeLikeVersion = forgeLikeVersion,
                installer = targetTempInstaller,
                tempMinecraftFolder = tempMinecraftFolder,
                inherit = processedInherit,
                loaderVersion = loaderVersion
            )
        }
    )
}

/**
 * [Reference PCL2](https://github.com/Hex-Dragon/PCL2/blob/bf6fa718c89e8615b947d1c639ed16a72ce125e0/Plain%20Craft%20Launcher%202/Pages/PageDownload/ModDownloadLib.vb#L1324-L1411)
 * 处理新版 Forge、NeoForge
 */
private suspend fun analyseNewForge(
    task: Task,
    downloader: BaseMinecraftDownloader,
    forgeLikeVersion: ForgeLikeVersion,
    installer: File,
    tempMinecraftFolder: File,
    inherit: String,
    loaderVersion: String
) {
    task.updateProgress(-1f)

    //解析 NeoForge 的支持库列表，并统一进行下载
    val (installProfile, installProfileString, versionString) = withContext(Dispatchers.IO) {
        ZipFile(installer).use { zip ->
            task.updateProgress(0.2f)

            val installProfileString = zip.readText("install_profile.json")
            val versionString = zip.readText("version.json")

            val installProfile = installProfileString.parseToJson()
            installProfile["libraries"]?.takeIf { it.isJsonArray }?.let { libraries ->
                val libraryList: List<GameManifest.Library> =
                    GSON.fromJson(libraries, object : TypeToken<List<GameManifest.Library>>() {}.type) ?: return@let

                for (library in libraryList) {
                    val path = artifactToPath(library) ?: continue
                    zip.getEntry("maven/$path")?.let { entry ->
                        val dest = File(tempMinecraftFolder, "libraries/$path")
                        zip.extractEntryToFile(entry, dest)
                    }
                }
            }

            installProfile["path"]?.takeIf { it.isJsonPrimitive }?.let { path ->
                val libraryPath = getLibraryPath(path.asString, tempMinecraftFolder.absolutePath)
                zip.getEntry("maven/$libraryPath")?.let { entry ->
                    val dest = File(tempMinecraftFolder, "libraries/$libraryPath")
                    zip.extractEntryToFile(entry, dest)
                }
            }

            Triple(installProfile, installProfileString, versionString)
        }
    }

    //合并为一个Json
    installProfile.merge(versionString.parseToJson())

    //计划下载 install_profile.json 内的所有支持库
    val libDownloader = GameLibDownloader(
        downloader = downloader,
        gameJson = installProfileString
    )
    libDownloader.schedule(task, File(tempMinecraftFolder, "libraries").ensureDirectory(), false)

    //添加 Mojang Mappings 下载信息
    task.updateProgress(0.4f)
    scheduleMojangMappings(
        mergedJson = installProfile,
        tempMinecraftDir = tempMinecraftFolder,
        tempVanillaJar = File(tempMinecraftFolder, "versions/$inherit/$inherit.jar"),
        tempInstaller = installer
    ) { url, sha1, targetFile, size ->
        libDownloader.scheduleDownload(
            url = url,
            sha1 = sha1,
            targetFile = targetFile,
            size = size
        )
    }

    task.updateProgress(0.8f)

    libDownloader.apply {
        val neoforgeVersionString = "${forgeLikeVersion.loaderName.lowercase()}-$inherit-$loaderVersion"
        //去除其中的原始 ForgeLike
        removeDownload { lib ->
            (lib.targetFile.name.endsWith("$neoforgeVersionString.jar") ||
             lib.targetFile.name.endsWith("$neoforgeVersionString-client.jar")).also {
                if (it) {
                    Log.i(
                        FORGE_LIKE_ANALYSE_ID,
                        "The download task has been removed from the scheduled downloads: \n" +
                                "url: ${lib.url}\n" +
                                "target path: ${lib.targetFile.absolutePath}"
                    )
                }
            }
        }
    }

    //开始下载 NeoForge 支持库
    libDownloader.download(task)

    task.updateProgress(1f)
}

/**
 * 解析并提交下载Mojang映射
 */
private suspend fun scheduleMojangMappings(
    mergedJson: JsonObject,
    tempMinecraftDir: File,
    tempVanillaJar: File,
    tempInstaller: File,
    schedule: (url: String, sha1: String?, targetFile: File, size: Long) -> Unit
) = withContext(Dispatchers.IO) {
    val tempDir = File(tempMinecraftDir, ".temp/forge_installer_cache").ensureDirectory()
    val vars = mutableMapOf<String, String>()

    ZipFile(tempInstaller).use { zip ->
        zip.readText("install_profile.json").parseToJson()["data"].asJsonObject?.let { data ->
            for ((key, value) in data.entrySet()) {
                if (value.isJsonObject) {
                    val client = value.asJsonObject["client"]
                    if (client != null && client.isJsonPrimitive) {
                        parseLiteral(
                            baseDir = tempMinecraftDir,
                            literal = client.asString,
                            plainConverter = { str ->
                                val dest: Path = Files.createTempFile(tempDir.toPath(), null, null)
                                val item = str
                                    .removePrefix("\\")
                                    .removePrefix("/")
                                    .replace("\\", "/")
                                zip.extractEntryToFile(item, dest.toFile())
                                dest.toString()
                            }
                        )?.let {
                            vars[key] = it
                        }
                    }
                }
            }
        }
    }

    vars += mapOf(
        "SIDE" to "client",
        "MINECRAFT_JAR" to tempVanillaJar.absolutePath,
        "MINECRAFT_VERSION" to tempVanillaJar.absolutePath,
        "ROOT" to tempMinecraftDir.absolutePath,
        "INSTALLER" to tempInstaller.absolutePath,
        "LIBRARY_DIR" to File(tempMinecraftDir, "libraries").absolutePath
    )

    parseProcessors(
        baseDir = tempMinecraftDir,
        jsonObject = mergedJson,
        vars = vars,
        schedule = schedule
    )
}

/**
 * [Reference HMCL](https://github.com/HMCL-dev/HMCL/blob/6e05b5ee58e67cd40e58c6f6002f3599897ca358/HMCLCore/src/main/java/org/jackhuang/hmcl/download/forge/ForgeNewInstallTask.java#L332-L360)
 */
private suspend fun parseProcessors(
    baseDir: File,
    jsonObject: JsonObject,
    vars: Map<String, String>,
    schedule: (url: String, sha1: String?, targetFile: File, size: Long) -> Unit
) = withContext(Dispatchers.IO) {
    val processors: List<ForgeLikeInstallProcessor> = jsonObject["processors"]?.asJsonArray?.let { processors ->
        val type = object : TypeToken<List<ForgeLikeInstallProcessor>>() {}.type
        GSON.fromJson(processors, type)
    } ?: return@withContext

    processors.map { processor ->
        parseOptions(baseDir, processor.getArgs(), vars)
    }.forEach { options ->
        if (options["task"] != "DOWNLOAD_MOJMAPS" || options["side"] != "client") return@forEach
        val version = options["version"] ?: return@forEach
        val output = options["output"] ?: return@forEach
        Log.i(FORGE_LIKE_ANALYSE_ID, "Patching DOWNLOAD_MOJMAPS task")

        val versionManifest = MinecraftVersions.getVersionManifest()
        versionManifest.versions.find { it.id == version }?.let { vanilla ->
            val manifest = withRetry(FORGE_LIKE_ANALYSE_ID, maxRetries = 1) {
                NetWorkUtils.fetchStringFromUrl(vanilla.url).parseTo(GameManifest::class.java)
            }
            manifest.downloads?.clientMappings?.let { mappings ->
                schedule(mappings.url, mappings.sha1, File(output), mappings.size)
                Log.i(FORGE_LIKE_ANALYSE_ID, "Mappings: ${mappings.url} (SHA1: ${mappings.sha1})")
            } ?: throw Exception("client_mappings download info not found")
        }
    }
}

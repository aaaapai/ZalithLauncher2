package com.movtery.zalithlauncher.game.download.game.forge

import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.components.jre.Jre
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.ForgeLikeVersion
import com.movtery.zalithlauncher.game.download.game.GameLibDownloader
import com.movtery.zalithlauncher.game.download.game.getLibraryPath
import com.movtery.zalithlauncher.game.download.game.models.ForgeLikeInstallProcessor
import com.movtery.zalithlauncher.game.download.game.models.toPath
import com.movtery.zalithlauncher.game.download.game.parseLibraryComponents
import com.movtery.zalithlauncher.game.download.jvm_server.runJvmRetryRuntimes
import com.movtery.zalithlauncher.game.version.download.BaseMinecraftDownloader
import com.movtery.zalithlauncher.path.LibPath
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.file.ensureDirectory
import com.movtery.zalithlauncher.utils.file.extractEntryToFile
import com.movtery.zalithlauncher.utils.file.extractFromZip
import com.movtery.zalithlauncher.utils.file.readText
import com.movtery.zalithlauncher.utils.json.parseToJson
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.string.isBiggerOrEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jackhuang.hmcl.util.DigestUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.zip.ZipFile

const val FORGE_LIKE_INSTALL_ID = "Install.ForgeLike"

/**
 * Forge Like 安装 Task
 * @param isNew 是否为新版本 Forge、NeoForge
 * @param tempFolderName 临时版本文件夹名称
 */
fun getForgeLikeInstallTask(
    isNew: Boolean,
    downloader: BaseMinecraftDownloader,
    forgeLikeVersion: ForgeLikeVersion,
    tempFolderName: String,
    tempInstaller: File,
    tempGameFolder: File,
    tempMinecraftDir: File,
    inherit: String
): Task {
    return Task.runTask(
        id = FORGE_LIKE_INSTALL_ID,
        task = { task ->
            val tempVanillaJar = File(tempMinecraftDir, "versions/$inherit/$inherit.jar")
            val tempVersionJson = File(tempMinecraftDir, "versions/$tempFolderName/$tempFolderName.json")
            if (isNew) { //新版 Forge、NeoForge
                //以 HMCL 的方式手动安装
                installNewForgeHMCLWay(
                    task = task,
                    forgeLikeVersion = forgeLikeVersion,
                    tempInstaller = tempInstaller,
                    tempGameFolder = tempGameFolder,
                    tempMinecraftDir = tempMinecraftDir,
                    tempVersionJson = tempVersionJson,
                    tempVanillaJar = tempVanillaJar
                )

//                installNewForgePCLWay(
//                    task = task,
//                    forgeLikeVersion = forgeLikeVersion,
//                    tempVersionJson = tempVersionJson,
//                    tempInstaller = tempInstaller,
//                    tempGameFolder = tempGameFolder,
//                    tempMinecraftDir = tempMinecraftDir
//                )
            } else { //旧版 Forge
                installOldForge(
                    task = task,
                    downloader = downloader,
                    tempInstaller = tempInstaller,
                    tempMinecraftDir = tempMinecraftDir,
                    tempFolderName = tempFolderName,
                    tempVersionJson = tempVersionJson,
                    inherit = inherit
                )
            }

            // 修复 bootstraplauncher 0.1.17+ 的启动问题
            progressIgnoreList(tempVersionJson)
        }
    )
}

/**
 * 用 HMCL 的方式安装新版 Forge、NeoForge
 */
private suspend fun installNewForgeHMCLWay(
    task: Task,
    forgeLikeVersion: ForgeLikeVersion,
    tempInstaller: File,
    tempGameFolder: File,
    tempMinecraftDir: File,
    tempVersionJson: File,
    tempVanillaJar: File
) = withContext(Dispatchers.IO) {
    task.updateProgress(-1f)

    val tempDir = File(tempMinecraftDir, ".temp/forge_installer_cache").ensureDirectory()
    val vars = mutableMapOf<String, String>()

    val installProfile = ZipFile(tempInstaller).use { zip ->
        val installProfile = zip.readText("install_profile.json").parseToJson()
        //解压版本Json
        zip.extractEntryToFile("version.json", tempVersionJson)

        task.updateProgress(0.2f, R.string.download_game_install_forgelike_preparing_mapping_file, forgeLikeVersion.loaderName)
        installProfile["data"].asJsonObject?.let { data ->
            for ((key, value) in data.entrySet()) {
                if (value.isJsonObject) {
                    val client = value.asJsonObject["client"]
                    if (client != null && client.isJsonPrimitive) {
                        lInfo("Attempting to recognize mapping: ${client.asString}")
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
                                lInfo("Extracting item %item to directory $dest")
                                dest.toString()
                            }
                        )?.let {
                            vars[key] = it
                            lInfo("Recognized as mapping $key - $it")
                        }
                    }
                }
            }
        }

        installProfile
    }

    task.updateProgress(1f, R.string.download_game_install_forgelike_preparing_mapping_file, forgeLikeVersion.loaderName)

    vars["SIDE"] = "client"
    vars["MINECRAFT_JAR"] = tempVanillaJar.absolutePath
    vars["MINECRAFT_VERSION"] = tempVanillaJar.absolutePath
    vars["ROOT"] = tempMinecraftDir.absolutePath
    vars["INSTALLER"] = tempInstaller.absolutePath
    vars["LIBRARY_DIR"] = File(tempMinecraftDir, "libraries").absolutePath

    val processors: List<ForgeLikeInstallProcessor> = installProfile["processors"]
        ?.asJsonArray
        ?.let { GSON.fromJson<List<ForgeLikeInstallProcessor>>(it, object : TypeToken<List<ForgeLikeInstallProcessor>>() {}.type) }
        ?: return@withContext

    runProcessors(
        task = task,
        tempMinecraftDir = tempMinecraftDir,
        tempGameDir = tempGameFolder,
        processors = processors,
        vars = vars
    )
}

/**
 * 以 PCL2 的方式安装新版 Forge、NeoForge
 * [Reference PCL2](https://github.com/Hex-Dragon/PCL2/blob/bf6fa718c89e8615b947d1c639ed16a72ce125e0/Plain%20Craft%20Launcher%202/Pages/PageDownload/ModDownloadLib.vb#L1412-L1479)
 */
@Suppress("unused")
private suspend fun installNewForgePCLWay(
    task: Task,
    forgeLikeVersion: ForgeLikeVersion,
    tempVersionJson: File,
    tempInstaller: File,
    tempGameFolder: File,
    tempMinecraftDir: File
) {
    //记录当前文件夹列表，稍后用于检测差异
    val dirsBeforeInstall = File(tempMinecraftDir, "versions").listFiles { file -> file.isDirectory } ?: emptyArray()
    val beforeLog = dirsBeforeInstall.joinToString(", ") { it.name }
    lInfo("All version folders before installation: $beforeLog")

    task.updateProgress(-1f, R.string.download_game_install_base_installing, forgeLikeVersion.loaderName)
    runJvmRetryRuntimes(
        FORGE_LIKE_INSTALL_ID,
        jvmArgs = "-javaagent:" +
                //使用 AWTBlockerAgent 禁用 AWT GUI 类调用
                LibPath.AWT_BLOCKER_AGENT.absolutePath + " " +
                "-cp " +
                LibPath.FORGE_INSTALLER.absolutePath + ":" +
                tempInstaller.absolutePath + " " +
                "com.bangbang93.ForgeInstaller" + " " +
                tempMinecraftDir.absolutePath,
        prefixArgs = { jre ->
            if (jre.majorVersion >= 9) {
                "--add-exports cpw.mods.bootstraplauncher/cpw.mods.bootstraplauncher=ALL-UNNAMED"
            } else {
                null
            }
        },
        jre = Jre.JRE_8,
        userHome = tempGameFolder.absolutePath.trimEnd('\\')
    )

    task.updateProgress(0.9f, R.string.download_game_install_base_installing, forgeLikeVersion.loaderName)

    //检测差异
    val currentArray = File(tempMinecraftDir, "versions").listFiles { file -> file.isDirectory } ?: emptyArray()
    val currentLog = dirsBeforeInstall.joinToString(", ") { it.name }
    lInfo("All version folders after installation: $currentLog")

    //过滤
    val deltaArray = currentArray.filter { file ->
        !dirsBeforeInstall.any { it.absolutePath == file.absolutePath } &&
                //过滤非(Neo)Forge的版本文件夹，同时也考虑安装器的bug导致生成的空文件夹的情况
                file.name.contains("forge", ignoreCase = true) && file.listFiles()?.isNotEmpty() == true
    }
    val filteredLog = dirsBeforeInstall.joinToString(", ") { it.name }
    lInfo("Version folders remaining after filtering: $filteredLog")

    when (deltaArray.size) {
        1 -> {
            //复制新增的 json 文件
            val newJson = deltaArray[0].listFiles()?.first()!!
            newJson.copyTo(tempVersionJson)
            lInfo("The newly added version JSON file has been copied: ${newJson.absolutePath} -> ${tempVersionJson.absolutePath}")
        }
        0 -> lInfo("The newly added version folder was not found.")
        else -> lWarning("There are multiple suspected new versions, but it's unclear: ${deltaArray.joinToString { it.name }}")
    }

    task.updateProgress(1f, null)
}

/**
 * [Reference PCL2](https://github.com/Hex-Dragon/PCL2/blob/bf6fa718c89e8615b947d1c639ed16a72ce125e0/Plain%20Craft%20Launcher%202/Pages/PageDownload/ModDownloadLib.vb#L1481-L1532)
 */
private suspend fun installOldForge(
    task: Task,
    downloader: BaseMinecraftDownloader,
    tempInstaller: File,
    tempMinecraftDir: File,
    tempFolderName: String,
    tempVersionJson: File,
    inherit: String
) = withContext(Dispatchers.IO) {
    val librariesFolder = File(tempMinecraftDir, "libraries")

    val versionInfo: JsonObject? = ZipFile(tempInstaller).use { zip ->
        task.updateProgress(0.2f)
        val installProfile = zip.readText("install_profile.json").parseToJson()

        task.updateProgress(0.4f)
        File(tempMinecraftDir, "versions/$tempFolderName").ensureDirectory()
        task.updateProgress(0.5f)

        if (!installProfile.has("install")) {
            lInfo("Starting the Forge installation, Legacy method A")

            //建立 Json 文件
            val jsonVersion = zip.readText(installProfile["json"].asString.trimStart('/')).parseToJson()
            jsonVersion.addProperty("id", tempFolderName)
            tempVersionJson.writeText(GSON.toJson(jsonVersion))
            task.updateProgress(0.6f)

            //解压支持库文件
            zip.extractFromZip("maven", librariesFolder)

            null
        } else {
            lInfo("Starting the Forge installation, Legacy method B")
            val artifact = installProfile["install"].asJsonObject["path"].asString
            val jarPath = getLibraryPath(artifact, baseFolder = tempMinecraftDir.absolutePath)

            val jarFile = File(jarPath)
            if (jarFile.exists()) jarFile.delete()

            //解压 Jar 文件
            zip.extractEntryToFile(installProfile["install"].asJsonObject["filePath"].asString, jarFile)
            task.updateProgress(0.9f)

            //建立 Json 文件
            val versionInfo = installProfile["versionInfo"].asJsonObject
            if (!versionInfo.has("inheritsFrom")) {
                versionInfo.addProperty("inheritsFrom", inherit)
            }
            tempVersionJson.writeText(GSON.toJson(versionInfo))

            versionInfo.apply {
                get("libraries").asJsonArray.removeAll {
                    val lib = it.asJsonObject
                    if (lib.has("name")) {
                        val name = lib["name"].asString
                        //过滤掉 path 对应的 library (这个是需要解压出去的，没法下载)
                        //比如 net.minecraftforge:forge:1.7.10-10.13.4.1614-1.7.10
                        name == artifact
                    } else false //保留
                }
            }
        }
    }
    task.updateProgress(1f)

    //判断是否需要补全 Forge 支持库
    versionInfo?.let { info ->
        val libDownloader = GameLibDownloader(
            downloader = downloader,
            gameJson = GSON.toJson(info)
        )
        libDownloader.schedule(
            task = task,
            targetDir = librariesFolder
        )
        //开始补全 Forge 支持库
        libDownloader.download(task)
    }
}

/**
 * [Reference HMCL](https://github.com/HMCL-dev/HMCL/blob/6e05b5ee58e67cd40e58c6f6002f3599897ca358/HMCLCore/src/main/java/org/jackhuang/hmcl/download/forge/ForgeNewInstallTask.java#L79-L178)
 */
private suspend fun runProcessors(
    task: Task,
    tempMinecraftDir: File,
    tempGameDir: File,
    processors: List<ForgeLikeInstallProcessor>,
    vars: Map<String, String>
): Unit = withContext(Dispatchers.IO) {
    //优先构建所有需要执行的命令，以便于更好的计算进度
    val commandList = processors.mapNotNull { processor ->
        val options = parseOptions(tempMinecraftDir, processor.getArgs(), vars)
        if (options["task"] == "DOWNLOAD_MOJMAPS" || !processor.isSide("client")) return@mapNotNull null

        val outputs: Map<String?, String?> = processor.getOutputs().mapKeys { (k, _) ->
            parseLiteral(tempMinecraftDir, k, vars)
        }.mapValues { (_, v) ->
            parseLiteral(tempMinecraftDir, v, vars)
        }.also {
            require(it.keys.none { key -> key == null } && it.values.none { v -> v == null }) {
                "Invalid forge installation configuration"
            }
            lInfo("Parsed output mappings for ${processor.javaClass.simpleName}: ${it.entries.joinToString("\n")}")
        }

        val anyMissing = outputs.any { (key, expectedHash) ->
            val artifact = tempMinecraftDir.toPath().resolve(key)
            if (!Files.exists(artifact)) return@any true

            val actualHash = Files.newInputStream(artifact).use { stream ->
                DigestUtils.digestToString("SHA-1", stream)
            }
            if (actualHash != expectedHash) {
                Files.delete(artifact)
                lInfo("Invalid artifact removed: $artifact")
                true
            } else false
        }

        if (outputs.isNotEmpty() && !anyMissing) return@mapNotNull null

        val jarPath = tempMinecraftDir.toPath().resolve("libraries").resolve(processor.getJar().toPath())
        require(Files.isRegularFile(jarPath)) { "Game processor file not found: $jarPath" }

        val mainClass = JarFile(jarPath.toFile()).use {
            it.manifest.mainAttributes.getValue(Attributes.Name.MAIN_CLASS)
        }.takeIf(String::isNotBlank)
            ?: throw Exception("Game processor jar missing Main-Class: $jarPath")

        val classpath = processor.getClasspath().map { lib ->
            tempMinecraftDir.toPath().resolve("libraries").resolve(lib.toPath()).also { path ->
                require(Files.isRegularFile(path)) { "Missing dependency: $path" }
            }.toString()
        } + jarPath.toString()

        //构建 JvmArgs
        val jvmArgs = buildList {
            add("-cp")
            add(classpath.joinToString(File.pathSeparator))
            add(mainClass)
            addAll(
                processor.getArgs().map { arg ->
                    parseLiteral(tempMinecraftDir, arg, vars)
                        ?: throw IOException("Invalid forge installation argument: $arg")
                }
            )
        }.joinToString(" ")

        Triple(processor, jvmArgs, outputs)
    }

    //正式开始执行命令
    commandList.forEachIndexed { index, commandTriple ->
        val processor = commandTriple.first
        val jvmArgs = commandTriple.second
        val outputs = commandTriple.third

        runJvmRetryRuntimes(
            logId = FORGE_LIKE_INSTALL_ID,
            jvmArgs = jvmArgs,
            prefixArgs = { null },
            jre = Jre.JRE_8,
            userHome = tempGameDir.absolutePath.trimEnd('\\')
        ) {
            val jarPath = processor.getJar().toPath()
            val jarName = File(jarPath).name

            val progress = index.toFloat() / commandList.size
            task.updateProgress(progress, R.string.download_game_install_base_installing, jarName)

            lInfo("Start to run $jarPath with args: $jvmArgs")
        }

        for ((key, value) in outputs) {
            val artifact = Paths.get(key)
            if (!Files.isRegularFile(artifact)) throw FileNotFoundException("File missing: $artifact")

            val code: String = Files.newInputStream(artifact).use { stream ->
                DigestUtils.digestToString("SHA-1", stream)
            }
            if (code != value) {
                Files.delete(artifact)
                throw IOException("Checksum mismatch: expected $value, got $code for $artifact")
            }
        }
    }
}

/**
 * [Fix following HMCL](https://github.com/HMCL-dev/HMCL/blob/6b78f56/HMCLCore/src/main/java/org/jackhuang/hmcl/download/MaintainTask.java#L232-L243)
 * bootstraplauncher 0.1.17 will only apply ignoreList to file name of libraries in classpath.
 * So we only fixes name of primary jar.
 */
private suspend fun progressIgnoreList(
    tempVersionJson: File
) = withContext(Dispatchers.IO) {
    val jsonObject = tempVersionJson.readText().parseToJson()

    val libraries = jsonObject["libraries"]?.takeIf { it.isJsonArray }?.asJsonArray ?: return@withContext

    val hasNewBootstrapLauncher = libraries
        .mapNotNull {
            je -> je.takeIf {
                je1 -> je1.isJsonObject
            }?.asJsonObject
                ?.get("name")
                ?.takeIf { !it.isJsonNull }
                ?.asString
        }
        .map { parseLibraryComponents(it) }
        .any { it.groupId == "cpw.mods" && it.artifactId == "bootstraplauncher" && it.version.isBiggerOrEqualTo("0.1.17") }

    if (!hasNewBootstrapLauncher) return@withContext

    val jvmArgs = jsonObject["arguments"]?.takeIf { it.isJsonObject }
        ?.asJsonObject?.get("jvm")?.takeIf { it.isJsonArray }?.asJsonArray ?: return@withContext

    val ignoreListIndex = jvmArgs.indexOfLast {
        it.isJsonPrimitive && it.asJsonPrimitive.isString && it.asString.startsWith("-DignoreList=")
    }.takeIf { it != -1 } ?: return@withContext

    //追加 ${primary_jar_name}
    val originalArg = jvmArgs[ignoreListIndex].asString
    jvmArgs[ignoreListIndex] = GSON.toJsonTree("$originalArg,\${primary_jar_name}")

    tempVersionJson.writeText(
        GSON.toJson(jsonObject)
    )
}
package com.movtery.zalithlauncher.game.download.game

import android.content.Context
import android.content.Intent
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskState
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.FabricLikeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.ForgeLikeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.modlike.ModVersion
import com.movtery.zalithlauncher.game.download.game.fabric.getFabricLikeCompleterTask
import com.movtery.zalithlauncher.game.download.game.fabric.getFabricLikeDownloadTask
import com.movtery.zalithlauncher.game.download.game.forge.getForgeLikeAnalyseTask
import com.movtery.zalithlauncher.game.download.game.forge.getForgeLikeDownloadTask
import com.movtery.zalithlauncher.game.download.game.forge.getForgeLikeInstallTask
import com.movtery.zalithlauncher.game.download.game.forge.isNeoForge
import com.movtery.zalithlauncher.game.download.game.forge.targetTempForgeLikeInstaller
import com.movtery.zalithlauncher.game.download.game.optifine.getOptiFineDownloadTask
import com.movtery.zalithlauncher.game.download.game.optifine.getOptiFineInstallTask
import com.movtery.zalithlauncher.game.download.game.optifine.getOptiFineModsDownloadTask
import com.movtery.zalithlauncher.game.download.game.optifine.targetTempOptiFineInstaller
import com.movtery.zalithlauncher.game.download.jvm_server.JVMSocketServer
import com.movtery.zalithlauncher.game.download.jvm_server.JvmService
import com.movtery.zalithlauncher.game.path.getGameHome
import com.movtery.zalithlauncher.game.version.download.BaseMinecraftDownloader
import com.movtery.zalithlauncher.game.version.download.MinecraftDownloader
import com.movtery.zalithlauncher.game.version.installed.VersionConfig
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.file.copyDirectoryContents
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * 游戏安装器
 * @param context 用于获取任务描述信息
 * @param info 安装游戏所需要的信息，包括 Minecraft id、自定义版本名称、Addon 列表
 * @param scope 在有生命周期管理的scope中执行安装任务
 */
class GameInstaller(
    private val context: Context,
    private val info: GameDownloadInfo,
    private val scope: CoroutineScope
) {
    private val _tasksFlow: MutableStateFlow<List<GameInstallTask>> = MutableStateFlow(emptyList())
    val tasksFlow: StateFlow<List<GameInstallTask>> = _tasksFlow

    /**
     * 当前游戏的安装任务
     */
    private var job: Job? = null

    /**
     * 基础下载器
     */
    private val downloader = BaseMinecraftDownloader(verifyIntegrity = true)

    /**
     * 目标游戏客户端目录（缓存）
     * versions/<client-name>/...
     */
    private var targetClientDir: File? = null

    /**
     * 目标游戏目录
     */
    private val targetGameFolder: File = File(getGameHome())

    /**
     * 安装 Minecraft 游戏
     * @param onInstalled 游戏已完成安装
     * @param onError 游戏安装失败
     */
    fun installGame(
        onInstalled: () -> Unit = {},
        onError: (th: Throwable) -> Unit = {},
        updateTasks: (List<GameInstallTask>) -> Unit = { tasks ->
            _tasksFlow.update { tasks }
        }
    ) {
        job = scope.launch(Dispatchers.IO) {
            installGameSuspend(
                onInstalled = {
                    updateTasks(emptyList())
                    onInstalled()
                },
                onError = onError,
                updateTasks = updateTasks
            )
        }
    }

    /**
     * 安装 Minecraft 游戏
     * @param onInstalled 游戏已完成安装
     * @param onError 游戏安装失败
     */
    suspend fun installGameSuspend(
        createIsolation: Boolean = true,
        onInstalled: suspend (targetClientDir: File) -> Unit = {},
        onError: (th: Throwable) -> Unit = {},
        updateTasks: (List<GameInstallTask>) -> Unit = { tasks ->
            _tasksFlow.update { tasks }
        }
    ) = withContext(Dispatchers.IO) {
        //目标版本目录
        val targetClientDir1 = VersionsManager.getVersionPath(info.customVersionName)
        targetClientDir = targetClientDir1
        val targetVersionJson = File(targetClientDir1, "${info.customVersionName}.json")
//        val targetVersionJar = File(targetClientDir1, "${info.customVersionName}.jar")

        //目标版本已经安装的情况
        if (targetVersionJson.exists()) {
            lDebug("The game has already been installed!")
            return@withContext
        }

        val tempGameDir = PathManager.DIR_CACHE_GAME_DOWNLOADER
        val tempMinecraftDir = File(tempGameDir, ".minecraft")
        val tempGameVersionsDir = File(tempMinecraftDir, "versions")
        val tempClientDir = File(tempGameVersionsDir, info.gameVersion)

        //ModLoader临时目录
        val optifineDir = info.optifine?.let { File(tempGameVersionsDir, it.version) }
        val forgeDir = info.forge?.let { File(tempGameVersionsDir, "forge-${it.versionName}") }
        val neoforgeDir = info.neoforge?.let { File(tempGameVersionsDir, "neoforge-${it.versionName}") }
        val fabricDir = info.fabric?.let { File(tempGameVersionsDir, "fabric-loader-${it.version}-${info.gameVersion}") }
        val quiltDir = info.quilt?.let { File(tempGameVersionsDir, "quilt-loader-${it.version}-${info.gameVersion}") }

        //Mods临时目录
        val tempModsDir = File(tempGameDir, ".temp_mods")

        val tasks: MutableList<GameInstallTask> = mutableListOf()

        //开始之前，应该先清理一次临时游戏目录，否则可能会影响安装结果
        tasks.add(
            GameInstallTask(
                context.getString(R.string.download_install_clear_temp),
                Task.runTask(
                    id = "Download.Game.ClearTemp",
                    task = {
                        clearTempGameDir()
                        //清理完成缓存目录后，创建新的缓存目录
                        tempClientDir.createDirAndLog()
                        optifineDir?.createDirAndLog()
                        forgeDir?.createDirAndLog()
                        neoforgeDir?.createDirAndLog()
                        fabricDir?.createDirAndLog()
                        quiltDir?.createDirAndLog()
                        tempModsDir.createDirAndLog()
                    }
                )
            )
        )

        //下载安装原版
        tasks.add(
            GameInstallTask(
                context.getString(R.string.download_game_install_vanilla, info.gameVersion),
                createMinecraftDownloadTask(info.gameVersion, tempGameVersionsDir)
            )
        )

        // OptiFine 安装
        info.optifine?.let { optifineVersion ->
            if (forgeDir == null && fabricDir == null) {
                val isNewVersion: Boolean = optifineVersion.inherit.contains("w") || optifineVersion.inherit.split(".")[1].toInt() >= 14
                val targetInstaller: File = targetTempOptiFineInstaller(tempGameDir, tempMinecraftDir, optifineVersion.fileName, isNewVersion)

                //将OptiFine作为版本下载，其余情况则作为Mod下载
                tasks.add(
                    GameInstallTask(
                        context.getString(R.string.download_game_install_base_download_file, ModLoader.OPTIFINE.displayName, info.optifine.displayName),
                        getOptiFineDownloadTask(
                            targetTempInstaller = targetInstaller,
                            optifine = optifineVersion
                        )
                    )
                )

                //安装 OptiFine
                tasks.add(
                    GameInstallTask(
                        context.getString(R.string.download_game_install_base_install, ModLoader.OPTIFINE.displayName),
                        getOptiFineInstallTask(
                            tempGameDir = tempGameDir,
                            tempMinecraftDir = tempMinecraftDir,
                            tempInstallerJar = targetInstaller,
                            isNewVersion = isNewVersion,
                            optifineVersion = optifineVersion
                        )
                    )
                )
            } else {
                //仅作为Mod进行下载
                tasks.add(
                    GameInstallTask(
                        context.getString(R.string.download_game_install_base_download_file, ModLoader.OPTIFINE.displayName, info.optifine.displayName),
                        getOptiFineModsDownloadTask(
                            optifine = optifineVersion,
                            tempModsDir = tempModsDir
                        )
                    )
                )
            }
        }

        // ForgeLike 安装
        info.forge?.let { forgeVersion ->
            createForgeLikeTask(
                forgeLikeVersion = forgeVersion,
                tempGameDir = tempGameDir,
                tempMinecraftDir = tempMinecraftDir,
                tempFolderName = forgeDir!!.name,
                addTask = { tasks.add(it) }
            )
        }
        info.neoforge?.let { neoforgeVersion ->
            createForgeLikeTask(
                forgeLikeVersion = neoforgeVersion,
                tempGameDir = tempGameDir,
                tempMinecraftDir = tempMinecraftDir,
                tempFolderName = neoforgeDir!!.name,
                addTask = { tasks.add(it) }
            )
        }

        // FabricLike 安装
        info.fabric?.let { fabricVersion ->
            createFabricLikeTask(
                fabricLikeVersion = fabricVersion,
                tempMinecraftDir = tempMinecraftDir,
                tempFolderName = fabricDir!!.name,
                addTask = { tasks.add(it) }
            )
        }
        info.fabricAPI?.let { apiVersion ->
            tasks.add(
                GameInstallTask(
                    context.getString(R.string.download_game_install_base_download_file, ModLoader.FABRIC_API.displayName, info.fabricAPI.displayName),
                    createModLikeDownloadTask(
                        tempModsDir = tempModsDir,
                        modVersion = apiVersion
                    )
                )
            )
        }
        info.quilt?.let { quiltVersion ->
            createFabricLikeTask(
                fabricLikeVersion = quiltVersion,
                tempMinecraftDir = tempMinecraftDir,
                tempFolderName = quiltDir!!.name,
                addTask = { tasks.add(it) }
            )
        }
        info.quiltAPI?.let { apiVersion ->
            tasks.add(
                GameInstallTask(
                    context.getString(R.string.download_game_install_base_download_file, ModLoader.QUILT_API.displayName, info.quiltAPI.displayName),
                    createModLikeDownloadTask(
                        tempModsDir = tempModsDir,
                        modVersion = apiVersion
                    )
                )
            )
        }

        tasks.add(
            GameInstallTask(
                context.getString(R.string.download_game_install_game_files_progress),
                //如果有非原版以外的任务，则需要进行处理安装（合并版本Json、迁移文件等）
                if (optifineDir != null || forgeDir != null || neoforgeDir != null || fabricDir != null || quiltDir != null || tempModsDir.listFiles()?.isNotEmpty() == true) {
                    createGameInstalledTask(
                        tempMinecraftDir = tempMinecraftDir,
                        targetMinecraftDir = targetGameFolder,
                        targetClientDir = targetClientDir1,
                        tempClientDir = tempClientDir,
                        tempModsDir = tempModsDir,
                        createIsolation = createIsolation,
                        optiFineFolder = optifineDir,
                        forgeFolder = forgeDir,
                        neoForgeFolder = neoforgeDir,
                        fabricFolder = fabricDir,
                        quiltFolder = quiltDir
                    )
                } else {
                    //仅仅下载了原版，只复制版本client文件
                    createVanillaFilesCopyTask(
                        tempMinecraftDir = tempMinecraftDir
                    )
                }
            )
        )

        updateTasks(tasks)

        //开始安装
        startInstall(
            tasks = tasks,
            onInstalled = {
                onInstalled(targetClientDir1)
                targetClientDir = null
            },
            onError = { th ->
                lWarning("Failed to install game!", th)
                clearTargetClient()
                onError(th)
            }
        )
    }

    private suspend fun startInstall(
        tasks: List<GameInstallTask>,
        onInstalled: suspend () -> Unit,
        onError: (th: Throwable) -> Unit
    ) = withContext(Dispatchers.Default) {
        //简易的TaskSystem实现
        for (task in tasks) {
            try {
                ensureActive()
                task.task.taskState = TaskState.RUNNING
                withContext(task.task.dispatcher) {
                    task.task.task(this, task.task)
                }
                task.task.taskState = TaskState.COMPLETED
            } catch (th: Throwable) {
                if (th is CancellationException) return@withContext
                task.task.onError(th)
                onError(th)
                //有任务出现异常，终止所有安装任务
                return@withContext
            } finally {
                task.task.onFinally()
            }
        }
        try {
            ensureActive()
            onInstalled()
        } catch (_: CancellationException) {
        }
    }

    fun cancelInstall() {
        job?.cancel()
        _tasksFlow.update { emptyList() }

        clearTargetClient()

        CoroutineScope(Dispatchers.Main).launch {
            //停止Jvm服务
            val intent = Intent(GlobalContext.applicationContext, JvmService::class.java)
            GlobalContext.applicationContext.stopService(intent)
            JVMSocketServer.stop()
        }
    }

    /**
     * 清除临时游戏目录
     */
    private suspend fun clearTempGameDir() = withContext(Dispatchers.IO) {
        PathManager.DIR_CACHE_GAME_DOWNLOADER.takeIf { it.exists() }?.let { folder ->
            FileUtils.deleteQuietly(folder)
            lInfo("Temporary game directory cleared.")
        }
    }

    /**
     * 安装失败、取消安装时，都应该清除目标客户端版本文件夹
     */
    private fun clearTargetClient() {
        val dirToDelete = targetClientDir //临时变量
        targetClientDir = null

        CoroutineScope(Dispatchers.IO).launch {
//            clearTempGameDir() 考虑到用户可能操作快，双线程清理同一个文件夹可能导致一些问题
            dirToDelete?.let {
                //直接清除上一次安装的目标目录
                FileUtils.deleteQuietly(it)
                lInfo("Successfully deleted version directory: ${it.name} at path: ${it.absolutePath}")
            }
        }
    }

    /**
     * 获取下载原版 Task
     */
    private fun createMinecraftDownloadTask(
        tempClientName: String,
        tempVersionsDir: File
    ): Task {
        val mcDownloader = MinecraftDownloader(
            context = context,
            version = info.gameVersion,
            customName = info.customVersionName,
            verifyIntegrity = true,
            downloader = downloader
        )

        return mcDownloader.getDownloadTask(tempClientName, tempVersionsDir)
    }

    /**
     * @param tempFolderName 临时ModLoader版本文件夹名称
     */
    private fun createForgeLikeTask(
        forgeLikeVersion: ForgeLikeVersion,
        loaderVersion: String = forgeLikeVersion.versionName,
        tempGameDir: File,
        tempMinecraftDir: File,
        tempFolderName: String,
        addTask: (GameInstallTask) -> Unit
    ) {
        //类似 1.19.3-41.2.8 格式，优先使用 Version 中要求的版本而非 Inherit（例如 1.19.3 却使用了 1.19 的 Forge）
        val (processedInherit, processedLoaderVersion) =
            if (
                !forgeLikeVersion.isNeoForge && loaderVersion.startsWith("1.") && loaderVersion.contains("-")
            ) {
                loaderVersion.substringBefore("-") to loaderVersion.substringAfter("-")
            } else {
                forgeLikeVersion.inherit to loaderVersion
            }

        val tempInstaller = targetTempForgeLikeInstaller(tempGameDir)
        //下载安装器
        addTask(
            GameInstallTask(
                context.getString(R.string.download_game_install_base_download_file, forgeLikeVersion.loaderName, processedLoaderVersion),
                getForgeLikeDownloadTask(tempInstaller, forgeLikeVersion)
            )
        )
        //分析与安装
        val isNew = forgeLikeVersion is NeoForgeVersion || !forgeLikeVersion.isLegacy

        if (isNew) {
            addTask(
                GameInstallTask(
                    context.getString(R.string.download_game_install_forgelike_analyse, forgeLikeVersion.loaderName),
                    getForgeLikeAnalyseTask(
                        downloader = downloader,
                        targetTempInstaller = tempInstaller,
                        forgeLikeVersion = forgeLikeVersion,
                        tempMinecraftFolder = tempMinecraftDir,
                        sourceInherit = info.gameVersion,
                        processedInherit = processedInherit,
                        loaderVersion = processedLoaderVersion
                    )
                )
            )
        }

        addTask(
            GameInstallTask(
                context.getString(R.string.download_game_install_base_install, forgeLikeVersion.loaderName),
                getForgeLikeInstallTask(
                    isNew = isNew,
                    downloader = downloader,
                    forgeLikeVersion = forgeLikeVersion,
                    tempFolderName = tempFolderName,
                    tempInstaller = tempInstaller,
                    tempGameFolder = tempGameDir,
                    tempMinecraftDir = tempMinecraftDir,
                    inherit = processedInherit
                )
            )
        )
    }

    private fun createFabricLikeTask(
        fabricLikeVersion: FabricLikeVersion,
        tempMinecraftDir: File,
        tempFolderName: String,
        addTask: (GameInstallTask) -> Unit
    ) {
        val tempVersionJson = File(tempMinecraftDir, "versions/$tempFolderName/$tempFolderName.json")

        //下载 Json
        addTask(
            GameInstallTask(
                context.getString(R.string.download_game_install_base_download_file, fabricLikeVersion.loaderName, fabricLikeVersion.version),
                getFabricLikeDownloadTask(
                    fabricLikeVersion = fabricLikeVersion,
                    tempVersionJson = tempVersionJson
                )
            )
        )

        //补全游戏库
        addTask(
            GameInstallTask(
                context.getString(R.string.download_game_install_forgelike_analyse, fabricLikeVersion.loaderName),
                getFabricLikeCompleterTask(
                    downloader = downloader,
                    tempMinecraftDir = tempMinecraftDir,
                    tempVersionJson= tempVersionJson
                )
            )
        )
    }

    private fun createModLikeDownloadTask(
        tempModsDir: File,
        modVersion: ModVersion
    ) = Task.runTask(
        id = "Download.Mods",
        task = {
            NetWorkUtils.downloadFileSuspend(
                url = modVersion.file.url,
                sha1 = modVersion.file.hashes.sha1,
                outputFile = File(tempModsDir, modVersion.file.fileName)
            )
        }
    )

    /**
     * 游戏带附加内容安装完成，合并版本Json、迁移游戏文件
     */
    private fun createGameInstalledTask(
        tempMinecraftDir: File,
        targetMinecraftDir: File,
        targetClientDir: File,
        tempClientDir: File,
        tempModsDir: File,
        createIsolation: Boolean = true,
        optiFineFolder: File? = null,
        forgeFolder: File? = null,
        neoForgeFolder: File? = null,
        fabricFolder: File? = null,
        quiltFolder: File? = null
    ) = Task.runTask(
        id = GAME_JSON_MERGER_ID,
        dispatcher = Dispatchers.IO,
        task = { task ->
            //合并版本 Json
            task.updateProgress(0.1f)
            mergeGameJson(
                info = info,
                outputFolder = targetClientDir,
                clientFolder = tempClientDir,
                optiFineFolder = optiFineFolder,
                forgeFolder = forgeFolder,
                neoForgeFolder = neoForgeFolder,
                fabricFolder = fabricFolder,
                quiltFolder = quiltFolder
            )

            //迁移游戏文件
            copyDirectoryContents(
                File(tempMinecraftDir, "libraries"),
                File(targetMinecraftDir, "libraries"),
                onProgress = { percentage ->
                    task.updateProgress(percentage)
                }
            )

            //复制客户端文件
            copyVanillaFiles(
                sourceGameFolder = tempMinecraftDir,
                sourceVersion = info.gameVersion,
                destinationGameFolder = targetGameFolder,
                targetVersion = info.customVersionName
            )

            //复制Mods
            tempModsDir.listFiles()?.let {
                val targetModsDir = File(targetClientDir, VersionFolders.MOD.folderName)
                it.forEach { modFile ->
                    modFile.copyTo(File(targetModsDir, modFile.name))
                }
                if (createIsolation) {
                    //开启版本隔离
                    VersionConfig.createIsolation(targetClientDir).save()
                }
            }

            //清除临时游戏目录
            task.updateProgress(-1f, R.string.download_install_clear_temp)
            clearTempGameDir()
        }
    )

    /**
     * 仅原本客户端文件复制任务 json、jar
     */
    private fun createVanillaFilesCopyTask(
        tempMinecraftDir: File
    ): Task {
        return Task.runTask(
            id = "VanillaFilesCopy",
            task = { task ->
                //复制客户端文件
                copyVanillaFiles(
                    sourceGameFolder = tempMinecraftDir,
                    sourceVersion = info.gameVersion,
                    destinationGameFolder = targetGameFolder,
                    targetVersion = info.customVersionName
                )

                //清除临时游戏目录
                task.updateProgress(-1f, R.string.download_install_clear_temp)
                clearTempGameDir()
            }
        )
    }

    private fun File.createDirAndLog(): File {
        this.mkdirs()
        lDebug("Created directory: $this")
        return this
    }
}
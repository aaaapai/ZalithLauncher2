package com.movtery.zalithlauncher.game.download.game

import android.content.Context
import android.content.Intent
import android.util.Log
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskState
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.FabricLikeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.ForgeLikeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
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
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
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
 */
class GameInstaller(
    private val context: Context,
    private val info: GameDownloadInfo
) {
    private val TAG: String = "GameInstaller"

    private val scope = CoroutineScope(Dispatchers.Default)
    private val _tasksFlow: MutableStateFlow<List<GameInstallTask>> = MutableStateFlow(emptyList())
    val tasksFlow: StateFlow<List<GameInstallTask>> = _tasksFlow

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
        onError: (th: Throwable) -> Unit = {}
    ) {
        if (_tasksFlow.value.isNotEmpty()) {
            //正在安装中，阻止这次安装请求
            return
        }

        //目标版本目录
        targetClientDir = VersionsManager.getVersionPath(info.customVersionName)
        val targetVersionJson = File(targetClientDir!!, "${info.customVersionName}.json")
//        val targetVersionJar = File(targetClientDir!!, "${info.customVersionName}.jar")

        //目标版本已经安装的情况
        if (targetVersionJson.exists()) {
            Log.d(TAG, "The game has already been installed!")
            return
        }

        scope.launch(Dispatchers.IO) {
            //开始之前，应该先清理一次临时游戏目录，否则可能会影响安装结果
            clearTempGameDir()

            val tempGameDir = PathManager.DIR_CACHE_GAME_DOWNLOADER
            val tempMinecraftDir = File(tempGameDir, ".minecraft")
            val tempGameVersionsDir = File(tempMinecraftDir, "versions")
            val tempClientDir by lazy {
                File(tempGameVersionsDir, info.gameVersion).createDirAndLog()
            }

            //ModLoader临时目录
            val optifineDir = info.optifine?.let { File(tempGameVersionsDir, it.version) }?.createDirAndLog()
            val forgeDir = info.forge?.let { File(tempGameVersionsDir, "forge-${it.versionName}") }?.createDirAndLog()
            val neoforgeDir = info.neoforge?.let { File(tempGameVersionsDir, "neoforge-${it.versionName}") }?.createDirAndLog()
            val fabricDir = info.fabric?.let { File(tempGameVersionsDir, "fabric-loader-${it.version}-${info.gameVersion}") }?.createDirAndLog()
            val quiltDir = info.quilt?.let { File(tempGameVersionsDir, "quilt-loader-${it.version}-${info.gameVersion}") }?.createDirAndLog()

            //Mods临时目录
            val tempModsDir = File(tempGameDir, ".temp_mods").createDirAndLog()

            val tasks: MutableList<GameInstallTask> = mutableListOf()

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
            info.quilt?.let { quiltVersion ->
                createFabricLikeTask(
                    fabricLikeVersion = quiltVersion,
                    tempMinecraftDir = tempMinecraftDir,
                    tempFolderName = quiltDir!!.name,
                    addTask = { tasks.add(it) }
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
                            targetClientDir = targetClientDir!!,
                            tempClientDir = tempClientDir,
                            tempModsDir = tempModsDir,
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

            _tasksFlow.update { tasks }

            //开始安装
            startInstall(
                onInstalled = {
                    _tasksFlow.update { emptyList() }
                    onInstalled()
                    targetClientDir = null
                },
                onError = { th ->
                    Log.w(TAG, "Failed to install game!", th)
                    clearTargetClient()
                    onError(th)
                }
            )
        }
    }

    private suspend fun startInstall(
        onInstalled: () -> Unit,
        onError: (th: Throwable) -> Unit
    ) = withContext(Dispatchers.Default) {
        //简易的TaskSystem实现
        for (task in _tasksFlow.value) {
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
        scope.cancel()
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
            Log.i(TAG, "Temporary game directory cleared.")
        }
    }

    /**
     * 安装失败、取消安装时，都应该清除目标客户端版本文件夹，和临时游戏目录
     */
    private fun clearTargetClient() {
        val dirToDelete = targetClientDir //临时变量
        targetClientDir = null

        CoroutineScope(Dispatchers.IO).launch {
            clearTempGameDir()
            dirToDelete?.let {
                //直接清除上一次安装的目标目录
                FileUtils.deleteQuietly(it)
                Log.i(TAG, "Successfully deleted version directory: ${it.name} at path: ${it.absolutePath}")
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
        outputName: String,
        modDownloadUrl: String
    ) = Task.runTask(
        id = "Download.Mods",
        task = {
            NetWorkUtils.downloadFileSuspend(
                url = modDownloadUrl,
                outputFile = File(tempModsDir, outputName)
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
            copyLibraries(
                File(tempMinecraftDir, "libraries"),
                File(targetMinecraftDir, "libraries"),
                onProgress = { progress ->
                    task.updateProgress(progress)
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
                val targetModsDir = File(targetClientDir, "mods")
                it.forEach { modFile ->
                    modFile.copyTo(File(targetModsDir, modFile.name))
                }
                //默认开启版本隔离
                VersionConfig.createIsolation(targetClientDir).save()
            }

            //清除临时游戏目录
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
            task = {
                //复制客户端文件
                copyVanillaFiles(
                    sourceGameFolder = tempMinecraftDir,
                    sourceVersion = info.gameVersion,
                    destinationGameFolder = targetGameFolder,
                    targetVersion = info.customVersionName
                )
            }
        )
    }

    private fun File.createDirAndLog(): File {
        this.mkdirs()
        Log.d(TAG, "Created directory: $this")
        return this
    }
}
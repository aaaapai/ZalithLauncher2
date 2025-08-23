package com.movtery.zalithlauncher.game.download.modpack.install

import android.content.Context
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskState
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersions
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.download.game.GameInstallTask
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.movtery.zalithlauncher.game.version.installed.VersionConfig
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.DownloadVersionInfo
import com.movtery.zalithlauncher.utils.file.copyDirectoryContents
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
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
 * 整合包安装器
 * @param info 选中的整合包的版本信息
 * @param scope 在有生命周期管理的scope中执行安装任务
 * @param waitForVersionName 等待用户输入版本名称
 */
class ModPackInstaller(
    private val context: Context,
    private val info: DownloadVersionInfo,
    private val scope: CoroutineScope,
    private val waitForVersionName: suspend (ModPackInfo) -> String
) {
    private val _tasksFlow: MutableStateFlow<List<GameInstallTask>> = MutableStateFlow(emptyList())
    val tasksFlow: StateFlow<List<GameInstallTask>> = _tasksFlow

    /**
     * 当前整合包的安装任务
     */
    private var job: Job? = null

    /**
     * 整合包文件解析出的信息
     */
    private lateinit var modpackInfo: ModPackInfo

    /**
     * 用户指定的预安装版本名称
     */
    private lateinit var targetVersionName: String

    /**
     * 即将下载的游戏版本的信息
     */
    private lateinit var gameDownloadInfo: GameDownloadInfo

    private var gameInstaller: GameInstaller? = null

    /**
     * 开始安装整合包
     */
    fun installModPack(
        onInstalled: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (_tasksFlow.value.isNotEmpty()) {
            //正在安装中，阻止这次安装请求
            return
        }

        job = scope.launch {
            installModPackSuspend(
                onInstalled = {
                    _tasksFlow.update { emptyList() }
                    onInstalled()
                },
                onError = { th ->
//                    clearTempDir()  考虑到用户可能操作快，双线程清理同一个文件夹可能导致一些问题
                    onError(th)
                }
            )
        }
    }

    private suspend fun installModPackSuspend(
        onInstalled: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        //临时游戏环境目录
        val tempModPackDir = PathManager.DIR_CACHE_MODPACK_DOWNLOADER
        val tempVersionsDir = File(tempModPackDir, "fkVersion")
        //整合包安装包文件
        val installerFile = File(tempModPackDir, "installer.zip")
        //icon临时文件
        val tempIconFile = File(tempModPackDir, "icon.png")

        val tasks = mutableListOf<GameInstallTask>()

        //清除上一次安装的缓存（如果有的话，可能会影响这次的安装结果）
        tasks.add(
            GameInstallTask(
                title = context.getString(R.string.download_install_clear_temp),
                task = Task.runTask(
                    id = "Download.ModPack.ClearTemp",
                    task = {
                        clearTempModPackDir()
                        //清理完成缓存目录后，创建新的缓存目录
                        tempModPackDir.createDirAndLog()
                        tempVersionsDir.createDirAndLog()
                        File(tempVersionsDir, VersionFolders.MOD.folderName).createDirAndLog() //创建临时模组目录
                    }
                )
            )
        )

        //下载整合包安装包
        tasks.add(
            GameInstallTask(
                title = context.getString(R.string.download_game_install_base_download_file2, info.displayName),
                task = Task.runTask(
                    id = "Download.ModPack.Installer",
                    task = { task ->
                        var downloadedSize = 0L
                        fun updateProgress() {
                            task.updateProgress((downloadedSize.toDouble() / info.fileSize.toDouble()).toFloat())
                        }
                        NetWorkUtils.downloadFileSuspend(
                            url = info.downloadUrl,
                            sha1 = info.sha1,
                            outputFile = installerFile,
                            sizeCallback = { size ->
                                downloadedSize += size
                                updateProgress()
                            }
                        )
                        //下载icon图片
                        task.updateProgress(-1f, null)
                        info.iconUrl?.let { iconUrl ->
                            NetWorkUtils.downloadFileSuspend(
                                url = iconUrl,
                                outputFile = tempIconFile
                            )
                        }
                    }
                )
            )
        )

        //解析整合包、解压整合包
        tasks.add(
            GameInstallTask(
                title = context.getString(R.string.download_modpack_install_parse),
                task = Task.runTask(
                    id = "Parse.ModPack",
                    task = { task ->
                        modpackInfo = parserModPack(
                            file = installerFile,
                            platform = info.platform,
                            targetFolder = tempVersionsDir,
                            task = task
                        )
                    }
                )
            )
        )

        //等待用户输入预安装版本名称
        tasks.add(
            GameInstallTask(
                title = context.getString(R.string.download_install_input_version_name),
                task = Task.runTask(
                    id = "Download.ModPack.WaitUserForVersionName",
                    task = { task ->
                        task.updateProgress(-1f)
                        targetVersionName = waitForVersionName(modpackInfo)
                    }
                )
            )
        )

        //下载整合包模组文件
        tasks.add(
            GameInstallTask(
                title = context.getString(R.string.download_modpack_download),
                task = Task.runTask(
                    id = "Download.ModPack.Mods",
                    dispatcher = Dispatchers.IO,
                    task = { task ->
                        val downloadTask = ModDownloader(modpackInfo.files)
                        downloadTask.startDownload(task)
                    }
                )
            )
        )

        //分析并匹配模组加载器信息，并构造出游戏安装信息
        tasks.add(
            GameInstallTask(
                title = context.getString(R.string.download_modpack_get_loaders),
                task = createRetrieveLoaderTask()
            )
        )

        _tasksFlow.update { tasks }

        //运行前面添加的任务
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

        //开始安装游戏！
        gameInstaller = GameInstaller(context, gameDownloadInfo, scope)
        gameInstaller!!.installGameSuspend(
            createIsolation = false, //不在这里开启版本隔离，后面单独设置版本
            onInstalled = { targetClientDir ->
                //最终整合包安装任务
                val finalTask = GameInstallTask(
                    title = context.getString(R.string.download_modpack_final_move),
                    task = createFinalInstallTask(
                        targetClientDir = targetClientDir,
                        tempVersionsDir = tempVersionsDir,
                        tempIconFile = tempIconFile
                    )
                )

                _tasksFlow.update { listOf(finalTask) }

                try {
                    ensureActive()
                    finalTask.task.taskState = TaskState.RUNNING
                    withContext(finalTask.task.dispatcher) {
                        finalTask.task.task(this, finalTask.task)
                    }
                    finalTask.task.taskState = TaskState.COMPLETED
                } catch (th: Throwable) {
                    if (th is CancellationException) return@installGameSuspend
                    finalTask.task.onError(th)
                    onError(th)
                    return@installGameSuspend
                } finally {
                    finalTask.task.onFinally()
                }

                //完成整合包的安装
                onInstalled()
            },
            onError = onError,
            updateTasks = { gameTasks ->
                _tasksFlow.update { gameTasks }
            }
        )
    }

    /**
     * 取消安装
     */
    fun cancelInstall() {
        job?.cancel()
        _tasksFlow.update { emptyList() }

        gameInstaller?.cancelInstall()
//        clearTempDir() 考虑到用户可能操作快，双线程清理同一个文件夹可能导致一些问题
    }

//    private fun clearTempDir() {
//        CoroutineScope(Dispatchers.IO).launch {
//            clearTempModPackDir()
//        }
//    }

    /**
     * 清理临时整合包版本目录
     */
    private suspend fun clearTempModPackDir() = withContext(Dispatchers.IO) {
        PathManager.DIR_CACHE_MODPACK_DOWNLOADER.takeIf { it.exists() }?.let { folder ->
            FileUtils.deleteQuietly(folder)
            lInfo("Temporary modpack directory cleared.")
        }
    }

    /**
     * 创建模组加载器解析匹配任务（调用前确保modpackInfo已经成功赋值）
     */
    private fun createRetrieveLoaderTask() = Task.runTask(
        id = "ModPack.Retrieve.Loader",
        task = { task ->
            val gameVersion = modpackInfo.gameVersion

            var gameInfo = GameDownloadInfo(
                gameVersion = gameVersion,
                customVersionName = targetVersionName
            )

            //匹配目标加载器版本，获取详细版本信息
            modpackInfo.loaders.forEach { (loader, version) ->
                when (loader) {
                    ModLoader.FORGE -> {
                        ForgeVersions.fetchForgeList(gameVersion)?.find {
                            it.versionName == version
                        }?.let { forgeVersion ->
                            gameInfo = gameInfo.copy(forge = forgeVersion)
                        }
                    }
                    ModLoader.NEOFORGE -> {
                        NeoForgeVersions.fetchNeoForgeList()?.find {
                            it.inherit == gameVersion && it.versionName == version
                        }?.let { neoforgeVersion ->
                            gameInfo = gameInfo.copy(neoforge = neoforgeVersion)
                        }
                    }
                    ModLoader.FABRIC -> {
                        FabricVersions.fetchFabricLoaderList(gameVersion)?.find {
                            it.version == version
                        }?.let { fabricVersion ->
                            gameInfo = gameInfo.copy(fabric = fabricVersion)
                        }
                    }
                    ModLoader.QUILT -> {
                        QuiltVersions.fetchQuiltLoaderList(gameVersion)?.find {
                            it.version == version
                        }?.let { quiltVersion ->
                            gameInfo = gameInfo.copy(quilt = quiltVersion)
                        }
                    }
                    else -> {
                        //不支持
                    }
                }
            }

            gameDownloadInfo = gameInfo
        }
    )

    /**
     * 创建最终安装任务
     */
    private fun createFinalInstallTask(
        targetClientDir: File,
        tempVersionsDir: File,
        tempIconFile: File
    ) = Task.runTask(
        id = "ModPack.Final.Install",
        dispatcher = Dispatchers.IO,
        task = { task ->
            task.updateProgress(-1f)
            //复制文件
            copyDirectoryContents(
                tempVersionsDir,
                targetClientDir
            ) { percentage ->
                task.updateProgress(percentage = percentage)
            }

            //复制整合包icon
            if (tempIconFile.exists() && tempIconFile.isFile) {
                val iconFile = VersionsManager.getVersionIconFile(targetClientDir)
                if (iconFile.exists()) FileUtils.deleteQuietly(iconFile)
                tempIconFile.copyTo(iconFile)
            }

            //创建版本信息
            VersionConfig.createIsolation(targetClientDir).apply {
                this.versionSummary = modpackInfo.summary ?: "" //整合包描述
            }.save()

            //清理临时整合包目录
            task.updateProgress(-1f, R.string.download_install_clear_temp)
            clearTempModPackDir()
        }
    )

    private fun File.createDirAndLog(): File {
        this.mkdirs()
        lDebug("Created directory: $this")
        return this
    }
}
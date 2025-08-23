package com.movtery.zalithlauncher.game.download.modpack.install

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.version.download.DownloadFailedException
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicLong

class ModDownloader(
    val mods: List<ModFile>,
    private val maxDownloadThreads: Int = 64
) {
    //文件下载进度计数
    private var downloadedFileCount: AtomicLong = AtomicLong(0)
    private var downloadedFileSize: AtomicLong = AtomicLong(0)
    private val downloadFailedTasks = mutableSetOf<ModFile>()

    suspend fun startDownload(task: Task) {
        downloadAll(
            task = task,
            taskMessageRes = R.string.download_modpack_download_mods
        )
        if (downloadFailedTasks.isNotEmpty()) {
            downloadedFileCount.set(0)
            downloadedFileSize.set(0L)
            downloadAll(
                task = task,
                tasks = downloadFailedTasks.toList(),
                taskMessageRes = R.string.download_modpack_download_mods_retry
            )
        }
        if (downloadFailedTasks.isNotEmpty()) throw DownloadFailedException()
        //清除任务信息
        task.updateProgress(1f, null)
    }

    private suspend fun downloadAll(
        task: Task,
        tasks: List<ModFile> = mods,
        taskMessageRes: Int,
        totalFileCount: Int = tasks.size
    ) = coroutineScope {
        downloadFailedTasks.clear()

        val semaphore = Semaphore(maxDownloadThreads)

        val downloadJobs = tasks.map { mod ->
            launch {
                semaphore.withPermit {
                    suspend fun download(file: ModFile) {
                        val urls = file.downloadUrls!!
                        val outputFile = file.outputFile!!
                        runCatching {
                            NetWorkUtils.downloadFromMirrorListSuspend(
                                urls = urls,
                                sha1 = file.sha1,
                                outputFile = outputFile
                            ) { size ->
                                downloadedFileSize.addAndGet(size)
                            }
                            //下载成功
                            downloadedFileCount.incrementAndGet()
                        }.onFailure { e ->
                            if (e is CancellationException) return@onFailure
                            lError("Download failed: ${outputFile.absolutePath}, urls: ${urls.joinToString(", ")}", e)
                            downloadFailedTasks.add(mod)
                        }
                    }

                    //在分析整合包阶段，可能无法及时快速的解析出模组的下载链接
                    //比如CurseForge整合包，基本上都是只携带projectID和fileID的
                    //但在解析阶段单独获取模组下载链接，非常耗时
                    mod.getFile?.let { getter ->
                        //在这里统一获取下载链接
                        //也能够蹭到下载器的多线程优化（很爽XD）
                        val file = getter()
                        if (file == null) downloadedFileCount.incrementAndGet()
                        else download(file)
                    } ?: run {
                        //只有已经获取到下载链接的 ModFile，getFile参数才是 null
                        //可以放心使用 downloadUrls 和 outputFile 参数
                        download(mod)
                    }
                }
            }
        }

        val progressJob = launch(Dispatchers.Main) {
            while (isActive) {
                try {
                    ensureActive()
                    val currentFileCount = downloadedFileCount.get()
                    task.updateProgress(
                        (currentFileCount.toFloat() / totalFileCount.toFloat()).coerceIn(0f, 1f),
                        taskMessageRes,
                        downloadedFileCount.get(), totalFileCount,
                        formatFileSize(downloadedFileSize.get())
                    )
                    delay(100)
                } catch (_: CancellationException) {
                    break //取消
                }
            }
        }

        try {
            downloadJobs.joinAll()
        } catch (e: CancellationException) {
            downloadJobs.forEach { it.cancel("Parent cancelled", e) }
        } finally {
            progressJob.cancel()
        }
    }
}
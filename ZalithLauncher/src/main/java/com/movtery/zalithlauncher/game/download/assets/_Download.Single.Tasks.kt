package com.movtery.zalithlauncher.game.download.assets

import android.content.Context
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.DownloadVersionInfo
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import okio.IOException
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

/**
 * 为一些版本下载单独的资源文件
 * @param info 要下载单独资源文件信息
 * @param versions 为哪些版本下载
 * @param folder 版本游戏目录下的相对路径
 * @param onFileCopied 文件已成功复制到版本游戏目录后 单独回调
 * @param onFileCancelled 文件安装已取消 单独回调
 */
fun downloadSingleForVersions(
    context: Context,
    info: DownloadVersionInfo,
    versions: List<Version>,
    folder: String,
    onFileCopied: suspend (zip: File, folder: File) -> Unit = { _, _ -> },
    onFileCancelled: (zip: File, folder: File) -> Unit = { _, _ -> },
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val cacheFile = File(File(PathManager.DIR_CACHE, "assets"), info.sha1 ?: info.fileName)

    downloadSingleFile(
        info = info,
        file = cacheFile,
        onDownloaded = { task ->
            task.updateProgress(-1f, R.string.download_assets_install_progress_installing, info.fileName)
            versions.forEach { version ->
                val targetFolder = File(version.getGameDir(), folder)
                val targetFile = File(targetFolder, info.fileName)
                if (targetFile.exists() && !targetFile.delete()) throw IOException("Failed to properly delete the existing target file.")
                cacheFile.copyTo(targetFile)
                onFileCopied(targetFile, targetFolder) //文件已复制回调
            }
        },
        onError = { e ->
            lWarning("An error occurred while downloading the resource files.", e)
            val message = mapExceptionToMessage(e).let { pair ->
                val args = pair.second
                if (args != null) {
                    context.getString(pair.first, *args)
                } else {
                    context.getString(pair.first)
                }
            }
            summitError(
                ErrorViewModel.ThrowableMessage(
                    title = context.getString(R.string.download_assets_install_failed),
                    message = message
                )
            )
        },
        onCancel = {
            FileUtils.deleteQuietly(cacheFile)
            versions.forEach { version ->
                val targetFolder = File(version.getGameDir(), folder)
                val targetFile = File(targetFolder, info.fileName)
                if (targetFile.exists()) FileUtils.deleteQuietly(targetFile)
                onFileCancelled(targetFile, targetFolder) //文件已取消回调
            }
        },
        onFinally = {
            lInfo("Attempting to clear cached resource files.")
            FileUtils.deleteQuietly(cacheFile)
        }
    )
}

private fun downloadSingleFile(
    info: DownloadVersionInfo,
    file: File,
    onDownloaded: suspend (Task) -> Unit,
    onError: (Throwable) -> Unit = {},
    onCancel: () -> Unit = {},
    onFinally: () -> Unit = {}
) {
    TaskSystem.submitTask(
        Task.runTask(
            id = info.sha1 ?: info.fileName,
            task = { task ->
                var downloadedSize = 0L

                //更新下载任务进度
                fun updateProgress() {
                    task.updateProgress(
                        (downloadedSize.toDouble() / info.fileSize.toDouble()).toFloat(),
                        R.string.download_assets_install_progress_downloading,
                        info.fileName,
                        formatFileSize(downloadedSize),
                        formatFileSize(info.fileSize),
                    )
                }
                updateProgress()

                NetWorkUtils.downloadFileSuspend(
                    url = info.downloadUrl,
                    sha1 = info.sha1,
                    outputFile = file.ensureParentDirectory(),
                    sizeCallback = { size ->
                        downloadedSize += size
                        updateProgress()
                    }
                )

                onDownloaded(task)
            },
            onError = onError,
            onCancel = onCancel,
            onFinally = onFinally
        )
    )
}

fun mapExceptionToMessage(e: Throwable): Pair<Int, Array<Any>?> {
    return when (e) {
        is HttpRequestTimeoutException -> Pair(R.string.error_timeout, null)
        is UnknownHostException, is UnresolvedAddressException -> Pair(R.string.error_network_unreachable, null)
        is ConnectException -> Pair(R.string.error_connection_failed, null)
        is ResponseException -> {
            when (e.response.status) {
                HttpStatusCode.Unauthorized -> Pair(R.string.error_unauthorized, null)
                HttpStatusCode.NotFound -> Pair(R.string.error_notfound, null)
                else -> Pair(R.string.error_client_error, arrayOf(e.response.status))
            }
        }
        else -> {
            val errorMessage = e.localizedMessage ?: e::class.simpleName ?: "Unknown error"
            Pair(R.string.error_unknown, arrayOf(errorMessage))
        }
    }
}
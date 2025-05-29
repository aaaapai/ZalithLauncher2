package com.movtery.zalithlauncher.game.version.download

import com.movtery.zalithlauncher.utils.file.compareSHA1
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runInterruptible
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException

class DownloadTask(
    val url: String,
    private val verifyIntegrity: Boolean,
    private val bufferSize: Int = 32768,
    val targetFile: File,
    val sha1: String?,
    /** 是否本身是可以被下载的，如果不可下载，则通过提供url尝试下载，如果失败则抛出 FileNotFoundException */
    val isDownloadable: Boolean,
    private val onDownloadFailed: (DownloadTask) -> Unit = {},
    private val onFileDownloadedSize: (Long) -> Unit = {},
    private val onFileDownloaded: () -> Unit = {}
) {
    suspend fun download() {
        //若目标文件存在，验证通过或关闭完整性验证时，跳过此次下载
        if (verifySha1()) {
            downloadedSize(FileUtils.sizeOf(targetFile))
            downloadedFile()
            return
        }

        runCatching {
            runInterruptible {
                NetWorkUtils.downloadFileWithHttp(
                    url = url,
                    outputFile = targetFile,
                    bufferSize = bufferSize
                ) { size ->
                    downloadedSize(size)
                }
            }
            downloadedFile()
        }.onFailure { e ->
            if (e is CancellationException) return@onFailure
            lError("Download failed: ${targetFile.absolutePath}, url: $url", e)
            if (!isDownloadable && e is FileNotFoundException) throw e
            onDownloadFailed(this)
        }
    }

    private fun downloadedSize(size: Long) {
        onFileDownloadedSize(size)
    }

    private fun downloadedFile() {
        onFileDownloaded()
    }

    /**
     * 若目标文件存在，验证完整性
     * @return 是否跳过此次下载
     */
    private fun verifySha1(): Boolean {
        if (targetFile.exists()) {
            sha1 ?: return true //sha1 不存在，可能目标无法被下载
            if (!verifyIntegrity || compareSHA1(targetFile, sha1)) {
                return true
            } else {
                FileUtils.deleteQuietly(targetFile)
            }
        }
        return false
    }
}
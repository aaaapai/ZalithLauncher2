/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.version.download

import com.movtery.zalithlauncher.path.DOWNLOAD_OKHTTP_CLIENT
import com.movtery.zalithlauncher.utils.network.downloadFromMirrorList
import com.movtery.zalithlauncher.utils.file.check7z
import com.movtery.zalithlauncher.utils.file.checkZip
import com.movtery.zalithlauncher.utils.file.compareSHA1
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.string.getMessageOrToString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private const val TAG = "DownloadTask"

class DownloadTask(
    val urls: List<String>,
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
    /**
     * 文件下载成功后执行的任务
     */
    var fileDownloadedTask: (suspend () -> Unit)? = null

    suspend fun download() {
        val file = targetFile
        // 若目标文件存在且校验通过，直接跳过
        if (file.exists() && verifySha1(file)) {
            downloadedSize(FileUtils.sizeOf(file))
            downloadedFile()
            return
        }

        // 使用自定义的 DOWNLOAD_OKHTTP_CLIENT 进行下载
        val client = DOWNLOAD_OKHTTP_CLIENT
        var lastException: Exception? = null

        for (url in urls) {
            try {
                Logger.debug(TAG, "尝试下载: $url")
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorMsg = "HTTP ${response.code} for $url"
                    Logger.error(TAG, errorMsg)
                    lastException = IOException(errorMsg)
                    continue
                }

                // 确保目标目录存在
                file.parentFile?.mkdirs()

                // 写入文件
                response.body?.let { body ->
                    file.outputStream().use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(bufferSize)
                            var bytesRead: Int
                            var totalBytes = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalBytes += bytesRead
                                onFileDownloadedSize(totalBytes)
                            }
                        }
                    }
                } ?: throw IOException("响应体为空")

                // 验证 SHA1（如果需要）
                if (sha1 != null && !compareSHA1(file, sha1)) {
                    FileUtils.deleteQuietly(file)
                    throw IOException("SHA1 校验失败")
                }

                // 下载成功
                downloadedSize(FileUtils.sizeOf(file))
                downloadedFile()
                return

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.error(TAG, "下载失败: $url", e)
                lastException = e
                // 继续尝试下一个镜像
            }
        }

        // 所有镜像都失败
        val finalError = lastException ?: IOException("所有镜像均无法下载")
        if (!isDownloadable && finalError is FileNotFoundException) throw finalError
        onDownloadFailed(this)
        throw finalError
    }

    private fun downloadedSize(size: Long) {
        onFileDownloadedSize(size)
    }

    private suspend fun downloadedFile() {
        onFileDownloaded()
        withContext(Dispatchers.IO) {
            fileDownloadedTask?.invoke()
        }
    }

    /**
     * 若目标文件存在，验证完整性
     * @return 是否跳过此次下载
     */
    private fun verifySha1(file: File): Boolean {
        if (!file.exists()) return false
        if (!verifyIntegrity) return true

        if (sha1.isNullOrBlank()) {
            //排除目标无法被下载的情况，比如Forge的client
            if (!isDownloadable) return true
            return verifyFileWithoutSha1(file)
        }

        return if (compareSHA1(file, sha1)) {
            true
        } else {
            FileUtils.deleteQuietly(file)
            false
        }
    }

    private fun verifyFileWithoutSha1(file: File): Boolean {
        val isAvailable = when (file.extension.lowercase()) {
            "zip", "jar" -> checkZip(file)
            "7z" -> check7z(file)
            else -> {
                //普通文件或是暂不受支持的压缩包
                return true
            }
        }

        if (isAvailable) {
            return true
        }

        FileUtils.deleteQuietly(file)
        return false
    }
}

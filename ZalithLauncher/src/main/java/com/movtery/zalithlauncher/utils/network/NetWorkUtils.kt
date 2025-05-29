package com.movtery.zalithlauncher.utils.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.net.toUri
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.path.UrlManager
import com.movtery.zalithlauncher.path.UrlManager.Companion.URL_USER_AGENT
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import okhttp3.Call
import org.apache.commons.io.FileUtils
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.HttpURLConnection
import java.net.URL

class NetWorkUtils {
    companion object {

        /**
         * @return 当前网络是否可用
         */
        fun isNetworkAvailable(context: Context): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            return activeNetwork != null && (
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false ||
                            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false ||
                            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ?: false
                    )
        }

        /**
         * 同步下载文件到本地
         * @param url 要下载的文件URL
         * @param outputFile 要保存的目标文件
         * @param bufferSize 缓冲区大小
         * @param sizeCallback 正在下载的大小回调
         */
        fun downloadFileWithHttp(
            url: String,
            outputFile: File,
            bufferSize: Int = 65536,
            sizeCallback: (Long) -> Unit = {}
        ) {
            outputFile.ensureParentDirectory()

            val conn = URL(url).openConnection() as HttpURLConnection

            conn.apply {
                readTimeout = UrlManager.TIME_OUT.first
                connectTimeout = UrlManager.TIME_OUT.first
                useCaches = true
                setRequestProperty("User-Agent", "Mozilla/5.0/$URL_USER_AGENT")
            }

            try {
                conn.connect()
                if (conn.responseCode !in 200..299) {
                    if (conn.responseCode == 404) throw FileNotFoundException("HTTP ${conn.responseCode} - ${conn.responseMessage}")
                    throw IOException("HTTP ${conn.responseCode} - ${conn.responseMessage}")
                }

                val contentLength = conn.contentLengthLong
                val buffer = ByteArray(bufferSize)

                conn.inputStream.use { inputStream ->
                    BufferedOutputStream(FileOutputStream(outputFile)).use { fos ->
                        var totalBytesRead = 0L
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            fos.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            sizeCallback(bytesRead.toLong())
                        }

                        if (contentLength != -1L && totalBytesRead != contentLength) {
                            throw IOException("Download incomplete. Expected $contentLength bytes, received $totalBytesRead bytes.")
                        }
                    }
                }
            } catch (e: Exception) {
                FileUtils.deleteQuietly(outputFile)
                when (e) {
                    is CancellationException, is InterruptedIOException -> {
                        lDebug("download task cancelled. url: $url")
                        return //取消了，不需要抛出异常
                    }
                    is FileNotFoundException -> throw e //目标不存在
                    else -> throw IOException("Download failed: $url", e)
                }
            }
        }

        /**
         * 同步下载文件到本地
         * @param url 要下载的文件URL
         * @param outputFile 要保存的目标文件
         * @param bufferSize 缓冲区大小
         * @param sizeCallback 正在下载的大小回调
         */
        suspend fun downloadFileSuspend(
            url: String,
            outputFile: File,
            bufferSize: Int = 65536,
            sizeCallback: (Long) -> Unit = {}
        ) = withContext(Dispatchers.IO) {
            runInterruptible {
                downloadFileWithHttp(
                    url = url,
                    outputFile = outputFile,
                    bufferSize = bufferSize,
                    sizeCallback = sizeCallback
                )
            }
        }

        /**
         * 同步获取 URL 返回的字符串内容
         * @param url 要请求的URL地址
         * @return 服务器返回的字符串内容
         * @throws IllegalArgumentException 当URL无效时
         * @throws IOException 当网络请求失败或响应解析失败时
         */
        @Throws(IOException::class, IllegalArgumentException::class)
        suspend fun fetchStringFromUrl(url: String): String = withContext(Dispatchers.IO) {
            call(url) { call ->
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} - ${response.message}")
                    }

                    return@call response.body
                        ?.use { it.string() }
                        ?: throw IOException("Empty response body")
                }
            }
        }

        private fun <T> call(url: String, call: (Call) -> T): T {
            val client = UrlManager.createOkHttpClient()
            val request = UrlManager.createRequestBuilder(url).build()

            return call(client.newCall(request))
        }

        /**
         * 展示一个提示弹窗，告知用户接下来将要在浏览器内访问的链接，用户可以选择不进行访问
         * @param link 要访问的链接
         */
        fun openLink(context: Context, link: String) {
            openLink(context, link, null)
        }

        /**
         * 展示一个提示弹窗，告知用户接下来将要在浏览器内访问的链接，用户可以选择不进行访问
         * @param link 要访问的链接
         * @param dataType 设置 intent 的数据以及显式 MIME 数据类型
         */
        fun openLink(context: Context, link: String, dataType: String?) {
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.generic_open_link)
                .setMessage(link)
                .setPositiveButton(R.string.generic_confirm) { _, _ ->
                    val uri = link.toUri()
                    val browserIntent: Intent
                    if (dataType != null) {
                        browserIntent = Intent(Intent.ACTION_VIEW)
                        browserIntent.setDataAndType(uri, dataType)
                    } else {
                        browserIntent = Intent(Intent.ACTION_VIEW, uri)
                    }
                    context.startActivity(browserIntent)
                }
                .setNegativeButton(R.string.generic_cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }
}
package com.movtery.zalithlauncher.game.skin

import com.google.gson.JsonObject
import com.movtery.zalithlauncher.path.UrlManager
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import com.movtery.zalithlauncher.utils.string.StringUtils
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class SkinFileDownloader {
    private val mClient = UrlManager.createOkHttpClient()

    /**
     * 尝试下载yggdrasil皮肤
     */
    @Throws(Exception::class)
    suspend fun yggdrasil(url: String, skinFile: File, uuid: String) {
        val profileJson = NetWorkUtils.fetchStringFromUrl("${url.removeSuffix("/")}/session/minecraft/profile/$uuid")
        val profileObject = GSON.fromJson(profileJson, JsonObject::class.java)
        val properties = profileObject.get("properties").asJsonArray
        val rawValue = properties.get(0).asJsonObject.get("value").asString

        val value = StringUtils.decodeBase64(rawValue)

        val valueObject = GSON.fromJson(value, JsonObject::class.java)
        val skinUrl = valueObject.get("textures").asJsonObject.get("SKIN").asJsonObject.get("url").asString

        downloadSkin(skinUrl, skinFile)
    }

    private fun downloadSkin(url: String, skinFile: File) {
        skinFile.parentFile?.apply {
            if (!exists()) mkdirs()
        }

        val request = Request.Builder()
            .url(url)
            .build()

        mClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Unexpected code $response")
            }

            try {
                response.body?.byteStream()?.use { inputStream ->
                    FileOutputStream(skinFile).use { outputStream ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                        }
                    }
                }
            } catch (e: Exception) {
                lError("Failed to download skin file", e)
            }
        }
    }
}
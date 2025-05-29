package com.movtery.zalithlauncher.game.addons.modloader.optifine

import com.movtery.zalithlauncher.game.addons.modloader.ResponseTooShortException
import com.movtery.zalithlauncher.path.UrlManager.Companion.GLOBAL_CLIENT
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.charsets.Charset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * [Some logic refers to PCL2](https://github.com/Hex-Dragon/PCL2/blob/44aea3e/Plain%20Craft%20Launcher%202/Modules/Minecraft/ModDownload.vb#L375-L409)
 */
object OptiFineVersions {
    private const val OPTIFINE_URL = "https://optifine.net"
    private const val OPTIFINE_DOWNLOAD_URL = "$OPTIFINE_URL/downloads"
    private const val OPTIFINE_ADLOADX_URL = "$OPTIFINE_URL/adloadx"

    private var cacheResult: List<OptiFineVersion>? = null

    /**
     * 获取 OptiFine 版本列表
     */
    suspend fun fetchOptiFineList(force: Boolean = false): List<OptiFineVersion>? = withContext(Dispatchers.Default) {
        if (!force) cacheResult?.let { return@withContext it }

        try {
            val response: HttpResponse = withContext(Dispatchers.IO) {
                GLOBAL_CLIENT.get(OPTIFINE_DOWNLOAD_URL)
            }

            val bytes: ByteArray = response.body()
            val html = bytes.toString(Charset.defaultCharset())
            if (html.length < 100) {
                throw ResponseTooShortException("Response too short")
            }
            val namePattern = Regex("<td class=['\"]colFile['\"]>([^<]+)</td>")
            val datePattern = Regex("<td class=['\"]colDate['\"]>([^<]+)</td>")
            val forgePattern = Regex("<td class=['\"]colForge['\"]>([^<]+)</td>")
            val jarPattern = Regex("adfoc\\.us[^>]+f=([^&]+\\.jar)")

            //提取所有匹配项
            val names = namePattern.findAll(html).map { it.groupValues[1].trim() }.toList()
            val dates = datePattern.findAll(html).map { it.groupValues[1].trim() }.toList()
            val forges = forgePattern.findAll(html).map { it.groupValues[1].trim() }.toList()
            val jars  = jarPattern.findAll(html).map { it.groupValues[1].trim() }.toList()

            if (names.size != dates.size || names.size != forges.size || names.size != jars.size) {
                lWarning("The number of parsed fields is inconsistent.")
                return@withContext emptyList()
            }

            val versions = mutableListOf<OptiFineVersion>()
            for (i in names.indices) {
                ensureActive()

                val rawName = jars[i].removeSuffix(".jar").removePrefix("preview_")
                val rawNameSpaced = rawName.replace("_", " ")

                val isPreview = jars[i].startsWith("preview_")

                val displayName = rawNameSpaced
                    .replace("OptiFine ", "")
                    .replace("HD U ", "")
                    .replace(".0 ", " ")

                val inherit = displayName.split(" ")[0]
                val fileName = (if (isPreview) "preview_" else "") + "$rawName.jar"

                val versionName = if (rawName.contains("$inherit.0_")) {
                    //OptiFine_1.9.0_HD_U_E7 -> 1.9-OptiFine_HD_U_E7
                    "${inherit}-${rawName.replace("$inherit.0_", "")}"
                } else {
                    //OptiFine_1.10.2_HD_U_C1 -> 1.10.2-OptiFine_HD_U_C1
                    "${inherit}-${rawName.replace("${inherit}_", "")}"
                }

                val rawDate = dates[i]
                val parts = rawDate.split('.')
                val formattedDate = if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else rawDate

                //提取Forge版本
                val forgeVersion = forges[i]
                    .takeIf { !it.contains("N/A") }
                    ?.removePrefix("Forge ")
                    ?.replace("#", "")
                    ?.trim()

                versions.add(
                    OptiFineVersion(
                        displayName = displayName,
                        fileName = fileName,
                        version = versionName,
                        inherit = inherit,
                        releaseDate = formattedDate,
                        forgeVersion = forgeVersion,
                        isPreview = isPreview
                    )
                )
            }
            versions
        } catch(e: CancellationException) {
            lDebug("Client cancelled.")
            null
        } catch (e: Exception) {
            lWarning("Failed to fetch OptiFine list!", e)
            throw e
        }
    }.also { result ->
        cacheResult = result
    }

    /**
     * 获取 OF 对应文件下载链接
     */
    suspend fun fetchOptiFineDownloadUrl(fileName: String): String? = withContext(Dispatchers.Default) {
        try {
            val response: HttpResponse = withContext(Dispatchers.IO) {
                GLOBAL_CLIENT.get("${OPTIFINE_ADLOADX_URL}?f=$fileName") {
                    contentType(ContentType.Text.Html)
                }
            }

            val html = response.bodyAsText()

            val match = Regex("""downloadx\?f=[^"'<>]+""").find(html)
            val downloadPath = match?.value

            if (downloadPath != null) {
                val finalUrl = "$OPTIFINE_URL/$downloadPath"
                return@withContext finalUrl
            } else {
                return@withContext null
            }

        } catch (e: Exception) {
            lWarning("Failed to fetch $fileName download url!", e)
            return@withContext null
        }
    }
}
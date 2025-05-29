package com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge

import com.movtery.zalithlauncher.game.addons.modloader.ResponseTooShortException
import com.movtery.zalithlauncher.path.UrlManager.Companion.GLOBAL_CLIENT
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.network.withRetry
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NeoForgeVersions {
    private const val TAG = "NeoForgeVersions"
    private var cacheResult: List<NeoForgeVersion>? = null

    /**
     * 获取 NeoForge 版本列表
     * [Reference PCL2](https://github.com/Hex-Dragon/PCL2/blob/44aea3e/Plain%20Craft%20Launcher%202/Modules/Minecraft/ModDownload.vb#L833-L849)
     */
    suspend fun fetchNeoForgeList(force: Boolean = false): List<NeoForgeVersion>? = withContext(Dispatchers.Default) {
        if (!force) cacheResult?.let { return@withContext it }

        try {
            val neoforge = withContext(Dispatchers.IO) {
                withRetry(TAG, maxRetries = 2) {
                    GLOBAL_CLIENT.get("https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge").body<String>()
                }
            }
            val legacyForge = withContext(Dispatchers.IO) {
                withRetry(TAG, maxRetries = 2) {
                    GLOBAL_CLIENT.get("https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/forge").body<String>()
                }
            }
            if (neoforge.length < 100 || legacyForge.length < 100) throw ResponseTooShortException("Response too short")

            parseEntries(neoforge, false) + parseEntries(legacyForge, true)
        } catch (e: CancellationException) {
            lDebug("Client cancelled.")
            null
        } catch (e: Exception) {
            lWarning("Failed to fetch neoforge list!", e)
            throw e
        }
    }.also {
        cacheResult = it
    }

    /**
     * 获取 NeoForge 对应版本的下载链接
     */
    fun getDownloadUrl(version: NeoForgeVersion) =
        "${version.baseUrl}-installer.jar"

    /**
     * [Reference PCL2](https://github.com/Hex-Dragon/PCL2/blob/44aea3e/Plain%20Craft%20Launcher%202/Modules/Minecraft/ModDownload.vb#L869-L878)
     */
    private fun parseEntries(json: String, isLegacy: Boolean): List<NeoForgeVersion> {
        val regex = Regex("""(?<=")(1\.20\.1-)?\d+\.\d+\.\d+(-beta)?(?=")""")
        return regex.findAll(json)
            .map { it.value }
            .filter { it != "47.1.82" } //这个版本虽然在版本列表中，但不能下载
            .map { NeoForgeVersion(it, isLegacy) }
            .sortedByDescending { it.forgeBuildVersion }
            .toList()
    }
}
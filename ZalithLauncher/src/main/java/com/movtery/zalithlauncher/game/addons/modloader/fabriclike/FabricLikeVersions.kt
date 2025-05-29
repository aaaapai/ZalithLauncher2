package com.movtery.zalithlauncher.game.addons.modloader.fabriclike

import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.models.FabricLikeGame
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.models.FabricLikeLoader
import com.movtery.zalithlauncher.path.UrlManager.Companion.GLOBAL_CLIENT
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.network.withRetry
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class FabricLikeVersions(private val baseUrl: String) {
    private var cacheGames: List<FabricLikeGame>? = null
    private var cacheLoaders: List<FabricLikeLoader>? = null

    /**
     * 通用 Loader 列表获取
     */
    protected suspend fun fetchLoaderList(
        force: Boolean,
        tag: String,
        mcVersion: String
    ): List<FabricLikeLoader>? = withContext(Dispatchers.Default) {
        try {
            val games: List<FabricLikeGame> = run {
                if (!force && cacheGames != null) return@run cacheGames!!
                withContext(Dispatchers.IO) {
                    withRetry(tag, maxRetries = 2) { GLOBAL_CLIENT.get(gameUrl).body() }
                }
            }.also {
                cacheGames = it
            }

            if (!games.any { it.version == mcVersion }) {
                lWarning("The version $mcVersion does not have a corresponding loader.")
                return@withContext null
            }

            if (!force && cacheLoaders != null) {
                cacheLoaders!!
            } else {
                withContext(Dispatchers.IO) {
                    withRetry(tag, maxRetries = 2) { GLOBAL_CLIENT.get(loaderUrl).body<List<FabricLikeLoader>>() }
                }
            }.also {
                cacheLoaders = it
            }
        } catch (e: CancellationException) {
            lDebug("Client cancelled.")
            null
        } catch (e: Exception) {
            lDebug("Failed to fetch loader list!", e)
            throw e
        }
    }

//    protected val installerUrl: String
//        get() = "$baseUrl/versions/installer"

    val loaderUrl: String
        get() = "$baseUrl/versions/loader"

    val gameUrl: String
        get() = "$baseUrl/versions/game"
}
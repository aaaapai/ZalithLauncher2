package com.movtery.zalithlauncher.game.addons.modloader.modlike

import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearch
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthFile.Companion.getPrimary
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthVersion
import com.movtery.zalithlauncher.utils.logging.Logger
import kotlinx.coroutines.CancellationException

/**
 * 模组版本管理类
 * @param modrinthID Modrinth 平台模组对应的 ID
 */
abstract class ModVersions(
    private val modrinthID: String
) {
    private var cacheVersions: List<ModrinthVersion>? = null

    /**
     * 获取特定版本的模组列表
     */
    suspend fun fetchVersionList(
        mcVersion: String,
        force: Boolean = false
    ): List<ModVersion>? {
        try {
            val versions = run {
                if (!force && cacheVersions != null) return@run cacheVersions!!
                PlatformSearch.getVersionsFromModrinth(modrinthID).also {
                    cacheVersions = it
                }
            }

            return versions.mapNotNull { version ->
                //仅保留版本号匹配的模组版本
                if (!version.gameVersions.contains(mcVersion)) return@mapNotNull null
                //仅保留主文件
                val file = version.files.getPrimary() ?: run {
                    Logger.lWarning("No file list available, skipping -> ${version.name}")
                    return@mapNotNull null
                }
                ModVersion(
                    inherit = mcVersion,
                    displayName = version.versionNumber,
                    version = version,
                    file = file
                )
            }
        } catch (_: CancellationException) {
            Logger.lDebug("Client cancelled.")
            return null
        } catch (e: Exception) {
            Logger.lDebug("Failed to fetch mod list! {mod id = $modrinthID}", e)
            throw e
        }
    }
}
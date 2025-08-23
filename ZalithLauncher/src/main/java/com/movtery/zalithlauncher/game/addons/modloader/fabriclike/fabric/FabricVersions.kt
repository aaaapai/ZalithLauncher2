package com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric

import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.FabricLikeVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.models.FabricLikeLoader

object FabricVersions : FabricLikeVersions(
    officialUrl = "https://meta.fabricmc.net/v2",
    mirrorUrl = "https://bmclapi2.bangbang93.com/fabric-meta/v2"
) {

    /**
     * 获取 Fabric 列表
     */
    suspend fun fetchFabricLoaderList(mcVersion: String, force: Boolean = false): List<FabricVersion>? {
        val list: List<FabricLikeLoader> = fetchLoaderList(force, "FabricVersions", mcVersion) ?: return null

        return list.map { loader ->
            FabricVersion(
                inherit = mcVersion,
                version = loader.version,
                stable = loader.stable
            )
        }
    }
}
package com.movtery.zalithlauncher.ui.screens.content.download.assets.search

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeResourcePackCategory
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthFeatures
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthResourcePackCategory
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadResourcePackScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.common.downloadResourcePackScreenKey
import com.movtery.zalithlauncher.ui.screens.content.downloadScreenKey
import kotlinx.serialization.Serializable

@Serializable
data object SearchResourcePackScreenKey : NavKey

@Composable
fun SearchResourcePackScreen(
    swapToDownload: (Platform, projectId: String) -> Unit = { _, _ -> }
) {
    SearchAssetsScreen(
        parentScreenKey = DownloadResourcePackScreenKey,
        parentCurrentKey = downloadScreenKey,
        screenKey = SearchResourcePackScreenKey,
        currentKey = downloadResourcePackScreenKey,
        platformClasses = PlatformClasses.RESOURCE_PACK,
        initialPlatform = Platform.MODRINTH,
        getCategories = { platform ->
            when (platform) {
                Platform.CURSEFORGE -> CurseForgeResourcePackCategory.entries
                Platform.MODRINTH -> ModrinthResourcePackCategory.entries
            }
        },
        mapCategories = { platform, string ->
            when (platform) {
                Platform.MODRINTH -> {
                    ModrinthResourcePackCategory.entries.find { it.facetValue() == string }
                        ?: ModrinthFeatures.entries.find { it.facetValue() == string }
                }
                Platform.CURSEFORGE -> {
                    CurseForgeResourcePackCategory.entries.find { it.describe() == string }
                }
            }
        },
        swapToDownload = swapToDownload
    )
}
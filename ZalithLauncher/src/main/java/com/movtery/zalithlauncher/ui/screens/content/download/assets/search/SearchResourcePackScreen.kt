package com.movtery.zalithlauncher.ui.screens.content.download.assets.search

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeResourcePackCategory
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthFeatures
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthResourcePackCategory
import com.movtery.zalithlauncher.ui.screens.NormalNavKey

@Composable
fun SearchResourcePackScreen(
    mainScreenKey: NavKey?,
    downloadScreenKey: NavKey?,
    downloadResourcePackScreenKey: NavKey,
    downloadResourcePackScreenCurrentKey: NavKey?,
    swapToDownload: (Platform, projectId: String, iconUrl: String?) -> Unit = { _, _, _ -> }
) {
    SearchAssetsScreen(
        mainScreenKey = mainScreenKey,
        parentScreenKey = downloadResourcePackScreenKey,
        parentCurrentKey = downloadScreenKey,
        screenKey = NormalNavKey.SearchResourcePack,
        currentKey = downloadResourcePackScreenCurrentKey,
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
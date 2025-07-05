package com.movtery.zalithlauncher.ui.screens.content.download.assets.search

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeModCategory
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.curseForgeModLoaderFilters
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthFeatures
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthModCategory
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.modrinthModLoaderFilters
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadModScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.common.downloadModScreenKey
import com.movtery.zalithlauncher.ui.screens.content.downloadScreenKey
import kotlinx.serialization.Serializable

@Serializable
data object SearchModScreenKey : NavKey

@Composable
fun SearchModScreen(
    swapToDownload: (Platform, projectId: String) -> Unit = { _, _ -> }
) {
    SearchAssetsScreen(
        parentScreenKey = DownloadModScreenKey,
        parentCurrentKey = downloadScreenKey,
        screenKey = SearchModScreenKey,
        currentKey = downloadModScreenKey,
        platformClasses = PlatformClasses.MOD,
        initialPlatform = Platform.MODRINTH,
        getCategories = { platform ->
            when (platform) {
                Platform.CURSEFORGE -> CurseForgeModCategory.entries
                Platform.MODRINTH -> ModrinthModCategory.entries
            }
        },
        enableModLoader = true,
        getModloaders = { platform ->
            when (platform) {
                Platform.CURSEFORGE -> curseForgeModLoaderFilters
                Platform.MODRINTH -> modrinthModLoaderFilters
            }
        },
        mapCategories = { platform, string ->
            when (platform) {
                Platform.MODRINTH -> {
                    ModrinthModCategory.entries.find { it.facetValue() == string }
                        ?: ModrinthFeatures.entries.find { it.facetValue() == string }
                }
                Platform.CURSEFORGE -> {
                    CurseForgeModCategory.entries.find { it.describe() == string }
                }
            }
        },
        swapToDownload = swapToDownload
    )
}
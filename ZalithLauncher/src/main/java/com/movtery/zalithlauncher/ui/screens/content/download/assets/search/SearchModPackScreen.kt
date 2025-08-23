package com.movtery.zalithlauncher.ui.screens.content.download.assets.search

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeModpackCategory
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.curseForgeModLoaderFilters
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthFeatures
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthModpackCategory
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.modrinthModLoaderFilters
import com.movtery.zalithlauncher.ui.screens.NormalNavKey

@Composable
fun SearchModPackScreen(
    mainScreenKey: NavKey?,
    downloadScreenKey: NavKey?,
    downloadModPackScreenKey: NavKey,
    downloadModPackScreenCurrentKey: NavKey?,
    swapToDownload: (Platform, projectId: String, iconUrl: String?) -> Unit = { _, _, _ -> }
) {
    SearchAssetsScreen(
        mainScreenKey = mainScreenKey,
        parentScreenKey = downloadModPackScreenKey,
        parentCurrentKey = downloadScreenKey,
        screenKey = NormalNavKey.SearchModPack,
        currentKey = downloadModPackScreenCurrentKey,
        platformClasses = PlatformClasses.MOD_PACK,
        initialPlatform = Platform.MODRINTH,
        getCategories = { platform ->
            when (platform) {
                Platform.CURSEFORGE -> CurseForgeModpackCategory.entries
                Platform.MODRINTH -> ModrinthModpackCategory.entries
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
                    ModrinthModpackCategory.entries.find { it.facetValue() == string }
                        ?: ModrinthFeatures.entries.find { it.facetValue() == string }
                }
                Platform.CURSEFORGE -> {
                    CurseForgeModpackCategory.entries.find { it.describe() == string }
                }
            }
        },
        swapToDownload = swapToDownload
    )
}
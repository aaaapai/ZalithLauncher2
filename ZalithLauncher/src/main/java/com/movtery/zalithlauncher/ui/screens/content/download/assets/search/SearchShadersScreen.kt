package com.movtery.zalithlauncher.ui.screens.content.download.assets.search

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeShadersCategory
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthFeatures
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthShadersCategory
import com.movtery.zalithlauncher.ui.screens.NormalNavKey

@Composable
fun SearchShadersScreen(
    mainScreenKey: NavKey?,
    downloadScreenKey: NavKey?,
    downloadShadersScreenKey: NavKey,
    downloadShadersScreenCurrentKey: NavKey?,
    swapToDownload: (Platform, projectId: String, iconUrl: String?) -> Unit = { _, _, _ -> }
) {
    SearchAssetsScreen(
        mainScreenKey = mainScreenKey,
        parentScreenKey = downloadShadersScreenKey,
        parentCurrentKey = downloadScreenKey,
        screenKey = NormalNavKey.SearchShaders,
        currentKey = downloadShadersScreenCurrentKey,
        platformClasses = PlatformClasses.SHADERS,
        initialPlatform = Platform.MODRINTH,
        getCategories = { platform ->
            when (platform) {
                Platform.CURSEFORGE -> CurseForgeShadersCategory.entries
                Platform.MODRINTH -> ModrinthShadersCategory.entries
            }
        },
        mapCategories = { platform, string ->
            when (platform) {
                Platform.MODRINTH -> {
                    ModrinthShadersCategory.entries.find { it.facetValue() == string }
                        ?: ModrinthFeatures.entries.find { it.facetValue() == string }
                }
                Platform.CURSEFORGE -> {
                    CurseForgeShadersCategory.entries.find { it.describe() == string }
                }
            }
        },
        swapToDownload = swapToDownload
    )
}
package com.movtery.zalithlauncher.ui.screens.content.download.assets.search

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeSavesCategory
import com.movtery.zalithlauncher.ui.screens.NormalNavKey

@Composable
fun SearchSavesScreen(
    mainScreenKey: NavKey?,
    downloadScreenKey: NavKey?,
    downloadSavesScreenKey: NavKey,
    downloadSavesScreenCurrentKey: NavKey?,
    swapToDownload: (Platform, projectId: String, iconUrl: String?) -> Unit = { _, _, _ -> }
) {
    SearchAssetsScreen(
        mainScreenKey = mainScreenKey,
        parentScreenKey = downloadSavesScreenKey,
        parentCurrentKey = downloadScreenKey,
        screenKey = NormalNavKey.SearchSaves,
        currentKey = downloadSavesScreenCurrentKey,
        platformClasses = PlatformClasses.SAVES,
        initialPlatform = Platform.CURSEFORGE,
        enablePlatform = false,
        getCategories = { CurseForgeSavesCategory.entries },
        mapCategories = { platform, string ->
            CurseForgeSavesCategory.entries.find { it.describe() == string }
        },
        swapToDownload = swapToDownload
    )
}
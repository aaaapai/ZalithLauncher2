package com.movtery.zalithlauncher.ui.screens.content.download.assets.search

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeSavesCategory
import com.movtery.zalithlauncher.ui.screens.content.download.DownloadSavesScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.common.downloadSavesScreenKey
import com.movtery.zalithlauncher.ui.screens.content.downloadScreenKey
import kotlinx.serialization.Serializable

@Serializable
data object SearchSavesScreenKey : NavKey

@Composable
fun SearchSavesScreen(
    swapToDownload: (Platform, projectId: String) -> Unit = { _, _ -> }
) {
    SearchAssetsScreen(
        parentScreenKey = DownloadSavesScreenKey,
        parentCurrentKey = downloadScreenKey,
        screenKey = SearchSavesScreenKey,
        currentKey = downloadSavesScreenKey,
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
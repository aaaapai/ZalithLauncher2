package com.movtery.zalithlauncher.ui.screens.content.download

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.download.DownloadAssetsScreen
import com.movtery.zalithlauncher.ui.screens.content.download.assets.download.DownloadAssetsScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchModPackScreen
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchModPackScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.common.downloadModBackStack
import com.movtery.zalithlauncher.ui.screens.content.download.common.downloadModPackBackStack
import com.movtery.zalithlauncher.ui.screens.content.download.common.downloadModPackScreenKey
import com.movtery.zalithlauncher.ui.screens.content.downloadScreenKey
import com.movtery.zalithlauncher.ui.screens.navigateTo
import kotlinx.serialization.Serializable

@Serializable
data object DownloadModPackScreenKey: NestedNavKey {
    override fun isLastScreen(): Boolean = downloadModPackBackStack.size <= 1
}

@Composable
fun DownloadModPackScreen() {
    val currentKey = downloadModPackBackStack.lastOrNull()

    LaunchedEffect(currentKey) {
        downloadModPackScreenKey = currentKey
    }

    NavDisplay(
        backStack = downloadModPackBackStack,
        modifier = Modifier.fillMaxSize(),
        onBack = {
            val key = downloadModPackBackStack.lastOrNull()
            if (key is NestedNavKey && !key.isLastScreen()) return@NavDisplay
            downloadModPackBackStack.removeLastOrNull()
        },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<SearchModPackScreenKey> {
                SearchModPackScreen { platform, projectId ->
                    downloadModPackBackStack.navigateTo(
                        DownloadAssetsScreenKey(platform, projectId, PlatformClasses.MOD_PACK)
                    )
                }
            }
            entry<DownloadAssetsScreenKey> { key ->
                DownloadAssetsScreen(
                    parentScreenKey = DownloadModPackScreenKey,
                    parentCurrentKey = downloadScreenKey,
                    currentKey = downloadModPackScreenKey,
                    key = key,
                    onItemClicked = { info ->

                    },
                    onDependencyClicked = { dep ->
                        downloadModBackStack.navigateTo(
                            DownloadAssetsScreenKey(dep.platform, dep.projectID, PlatformClasses.MOD_PACK)
                        )
                    }
                )
            }
        }
    )
}
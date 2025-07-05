package com.movtery.zalithlauncher.ui.screens.content.download

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.movtery.zalithlauncher.game.download.assets.downloadSingleForVersions
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.download.DownloadAssetsScreen
import com.movtery.zalithlauncher.ui.screens.content.download.assets.download.DownloadAssetsScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.DownloadSingleOperation
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchModScreen
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchModScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.common.downloadModBackStack
import com.movtery.zalithlauncher.ui.screens.content.download.common.downloadModScreenKey
import com.movtery.zalithlauncher.ui.screens.content.downloadScreenKey
import com.movtery.zalithlauncher.ui.screens.navigateTo
import kotlinx.serialization.Serializable

@Serializable
data object DownloadModScreenKey: NestedNavKey {
    override fun isLastScreen(): Boolean = downloadModBackStack.size <= 1
}

@Composable
fun DownloadModScreen() {
    val currentKey = downloadModBackStack.lastOrNull()

    LaunchedEffect(currentKey) {
        downloadModScreenKey = currentKey
    }

    val context = LocalContext.current

    //下载资源操作
    var operation by remember { mutableStateOf<DownloadSingleOperation>(DownloadSingleOperation.None) }
    DownloadSingleOperation(
        operation = operation,
        changeOperation = { operation = it },
        doInstall = { info, versions ->
            downloadSingleForVersions(
                context = context,
                info = info,
                versions = versions,
                folder = "mods"
            )
        }
    )

    NavDisplay(
        backStack = downloadModBackStack,
        modifier = Modifier.fillMaxSize(),
        onBack = {
            val key = downloadModBackStack.lastOrNull()
            if (key is NestedNavKey && !key.isLastScreen()) return@NavDisplay
            downloadModBackStack.removeLastOrNull()
        },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<SearchModScreenKey> {
                SearchModScreen { platform, projectId ->
                    downloadModBackStack.navigateTo(
                        DownloadAssetsScreenKey(platform, projectId, PlatformClasses.MOD)
                    )
                }
            }
            entry<DownloadAssetsScreenKey> { key ->
                DownloadAssetsScreen(
                    parentScreenKey = DownloadModScreenKey,
                    parentCurrentKey = downloadScreenKey,
                    currentKey = downloadModScreenKey,
                    key = key,
                    onItemClicked = { info ->
                        operation = DownloadSingleOperation.SelectVersion(info)
                    },
                    onDependencyClicked = { dep ->
                        downloadModBackStack.navigateTo(
                            DownloadAssetsScreenKey(dep.platform, dep.projectID, PlatformClasses.MOD)
                        )
                    }
                )
            }
        }
    )
}
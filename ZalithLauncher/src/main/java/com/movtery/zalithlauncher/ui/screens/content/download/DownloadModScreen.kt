package com.movtery.zalithlauncher.ui.screens.content.download

import androidx.compose.foundation.layout.Box
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
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.movtery.zalithlauncher.game.download.assets.downloadSingleForVersions
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.download.DownloadAssetsScreen
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.DownloadSingleOperation
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchModScreen
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel

@Composable
fun DownloadModScreen(
    key: NestedNavKey.DownloadMod,
    mainScreenKey: NavKey?,
    downloadScreenKey: NavKey?,
    downloadModScreenKey: NavKey?,
    onCurrentKeyChange: (NavKey?) -> Unit,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        onCurrentKeyChange(stackTopKey)
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
                folder = VersionFolders.MOD.folderName,
                summitError = summitError
            )
        }
    )

    if (backStack.isNotEmpty()) {
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize(),
            onBack = {
                onBack(backStack)
            },
            entryDecorators = listOf(
                rememberSceneSetupNavEntryDecorator(),
                rememberSavedStateNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<NormalNavKey.SearchMod> {
                    SearchModScreen(
                        mainScreenKey = mainScreenKey,
                        downloadScreenKey = downloadScreenKey,
                        downloadModScreenKey = key,
                        downloadModScreenCurrentKey = downloadModScreenKey
                    ) { platform, projectId, _ ->
                        backStack.navigateTo(
                            NormalNavKey.DownloadAssets(platform, projectId, PlatformClasses.MOD)
                        )
                    }
                }
                entry<NormalNavKey.DownloadAssets> { assetsKey ->
                    DownloadAssetsScreen(
                        mainScreenKey = mainScreenKey,
                        parentScreenKey = key,
                        parentCurrentKey = downloadScreenKey,
                        currentKey = downloadModScreenKey,
                        key = assetsKey,
                        onItemClicked = { info ->
                            operation = DownloadSingleOperation.SelectVersion(info)
                        },
                        onDependencyClicked = { dep ->
                            backStack.navigateTo(
                                NormalNavKey.DownloadAssets(dep.platform, dep.projectID, PlatformClasses.MOD)
                            )
                        }
                    )
                }
            }
        )
    } else {
        Box(Modifier.fillMaxSize())
    }
}
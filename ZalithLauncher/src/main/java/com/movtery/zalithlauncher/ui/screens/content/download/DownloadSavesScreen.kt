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
import com.movtery.zalithlauncher.game.download.assets.install.unpackSaveZip
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.download.DownloadAssetsScreen
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.DownloadSingleOperation
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchSavesScreen
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import java.io.File

@Composable
fun DownloadSavesScreen(
    key: NestedNavKey.DownloadSaves,
    mainScreenKey: NavKey?,
    downloadScreenKey: NavKey?,
    downloadSavesScreenKey: NavKey?,
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
                folder = VersionFolders.SAVES.folderName,
                onFileCopied = { file, folder ->
                    unpackSaveZip(
                        zipFile = file,
                        targetPath = folder
                    )
                },
                onFileCancelled = { file, folder ->
                    CoroutineScope(Dispatchers.IO).launch {
                        FileUtils.deleteQuietly(
                            File(folder, file.nameWithoutExtension)
                        )
                    }
                },
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
                entry<NormalNavKey.SearchSaves> {
                    SearchSavesScreen(
                        mainScreenKey = mainScreenKey,
                        downloadScreenKey = downloadScreenKey,
                        downloadSavesScreenKey = key,
                        downloadSavesScreenCurrentKey = downloadSavesScreenKey
                    ) { platform, projectId, _ ->
                        backStack.navigateTo(
                            NormalNavKey.DownloadAssets(platform, projectId, PlatformClasses.SAVES)
                        )
                    }
                }
                entry<NormalNavKey.DownloadAssets> { assetsKey ->
                    DownloadAssetsScreen(
                        mainScreenKey = mainScreenKey,
                        parentScreenKey = key,
                        parentCurrentKey = downloadScreenKey,
                        currentKey = downloadSavesScreenKey,
                        key = assetsKey,
                        onItemClicked = { info ->
                            operation = DownloadSingleOperation.SelectVersion(info)
                        }
                    )
                }
            }
        )
    } else {
        Box(Modifier.fillMaxSize())
    }
}
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
import com.movtery.zalithlauncher.ui.screens.content.download.assets.install.unpackSaveZip
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchSavesScreen
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchSavesScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.common.downloadModBackStack
import com.movtery.zalithlauncher.ui.screens.content.download.common.downloadSavesBackStack
import com.movtery.zalithlauncher.ui.screens.content.download.common.downloadSavesScreenKey
import com.movtery.zalithlauncher.ui.screens.content.downloadScreenKey
import com.movtery.zalithlauncher.ui.screens.navigateTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.apache.commons.io.FileUtils
import java.io.File

@Serializable
data object DownloadSavesScreenKey: NestedNavKey {
    override fun isLastScreen(): Boolean = downloadSavesBackStack.size <= 1
}

@Composable
fun DownloadSavesScreen() {
    val currentKey = downloadSavesBackStack.lastOrNull()

    LaunchedEffect(currentKey) {
        downloadSavesScreenKey = currentKey
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
                folder = "saves",
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
                }
            )
        }
    )

    NavDisplay(
        backStack = downloadSavesBackStack,
        modifier = Modifier.fillMaxSize(),
        onBack = {
            val key = downloadSavesBackStack.lastOrNull()
            if (key is NestedNavKey && !key.isLastScreen()) return@NavDisplay
            downloadSavesBackStack.removeLastOrNull()
        },
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<SearchSavesScreenKey> {
                SearchSavesScreen { platform, projectId ->
                    downloadSavesBackStack.navigateTo(
                        DownloadAssetsScreenKey(platform, projectId, PlatformClasses.SAVES)
                    )
                }
            }
            entry<DownloadAssetsScreenKey> { key ->
                DownloadAssetsScreen(
                    parentScreenKey = DownloadSavesScreenKey,
                    parentCurrentKey = downloadScreenKey,
                    currentKey = downloadSavesScreenKey,
                    key = key,
                    onItemClicked = { info ->
                        operation = DownloadSingleOperation.SelectVersion(info)
                    },
                    onDependencyClicked = { dep ->
                        downloadModBackStack.navigateTo(
                            DownloadAssetsScreenKey(dep.platform, dep.projectID, PlatformClasses.SAVES)
                        )
                    }
                )
            }
        }
    )
}
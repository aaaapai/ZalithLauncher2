package com.movtery.zalithlauncher.ui.screens.content.download.assets.elements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.content.elements.CommonVersionInfoLayout

/**
 * 操作状态：下载单个资源文件
 */
sealed interface DownloadSingleOperation {
    data object None : DownloadSingleOperation
    /** 选择版本 */
    data class SelectVersion(val info: DownloadVersionInfo) : DownloadSingleOperation
    /** 安装 */
    data class Install(val info: DownloadVersionInfo, val versions: List<Version>) : DownloadSingleOperation
}

@Composable
fun DownloadSingleOperation(
    operation: DownloadSingleOperation,
    changeOperation: (DownloadSingleOperation) -> Unit,
    doInstall: (DownloadVersionInfo, List<Version>) -> Unit
) {
    when (operation) {
        DownloadSingleOperation.None -> {}
        is DownloadSingleOperation.SelectVersion -> {
            SelectVersionToDownloadDialog(
                onDismiss = {
                    changeOperation(DownloadSingleOperation.None)
                },
                onInstall = { versions ->
                    changeOperation(DownloadSingleOperation.Install(operation.info, versions))
                }
            )
        }
        is DownloadSingleOperation.Install -> {
            doInstall(operation.info, operation.versions)
            changeOperation(DownloadSingleOperation.None)
        }
    }
}

@Composable
fun SelectVersionToDownloadDialog(
    onDismiss: () -> Unit,
    onInstall: (List<Version>) -> Unit
) {
    val versions by VersionsManager.versions.collectAsState()
    val version = VersionsManager.currentVersion

    if (version == null || versions.isEmpty()) {
        SimpleAlertDialog(
            title = stringResource(R.string.generic_warning),
            text = stringResource(R.string.download_assets_no_installed_versions),
            confirmText = stringResource(R.string.generic_go_it),
            onDismiss = onDismiss
        )
    } else {
        //当前选择的版本，将会把资源安装到该版本
        val selectedVersions = remember { mutableStateListOf(version) }

        Dialog(
            onDismissRequest = onDismiss
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.download_assets_install_assets_for_versions),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.size(16.dp))

                    //选择游戏版本
                    ChoseGameVersionLayout(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        versions = versions,
                        selectedVersions = selectedVersions,
                        onVersionSelected = { selectedVersions.add(it) },
                        onVersionUnSelected = { selectedVersions.remove(it) }
                    )

                    Spacer(modifier = Modifier.size(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(0.5f),
                            onClick = onDismiss
                        ) {
                            Text(text = stringResource(R.string.generic_cancel))
                        }
                        Button(
                            modifier = Modifier.weight(0.5f),
                            onClick = {
                                if (selectedVersions.isNotEmpty()) {
                                    onInstall(selectedVersions)
                                }
                            }
                        ) {
                            Text(text = stringResource(R.string.download_install))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoseGameVersionLayout(
    modifier: Modifier = Modifier,
    versions: List<Version>,
    selectedVersions: List<Version>,
    onVersionSelected: (Version) -> Unit,
    onVersionUnSelected: (Version) -> Unit,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation
    ) {
        if (versions.isNotEmpty()) {
            val listState = rememberLazyListState()

            LaunchedEffect(Unit) {
                versions.indexOf(selectedVersions[0]).takeIf { it != -1 }?.let { index ->
                    listState.animateScrollToItem(index)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                state = listState
            ) {
                items(versions) { version ->
                    SelectVersionListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 4.dp),
                        version = version,
                        checked = selectedVersions.contains(version),
                        onChose = {
                            onVersionSelected(version)
                        },
                        onCancel = {
                            onVersionUnSelected(version)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectVersionListItem(
    modifier: Modifier = Modifier,
    version: Version,
    checked: Boolean,
    onChose: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(
                onClick = {
                    if (checked) {
                        onCancel()
                    } else {
                        onChose()
                    }
                }
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = {
                if (it) {
                    onChose()
                } else {
                    onCancel()
                }
            }
        )
        CommonVersionInfoLayout(
            modifier = Modifier.weight(1f),
            version = version
        )
    }
}
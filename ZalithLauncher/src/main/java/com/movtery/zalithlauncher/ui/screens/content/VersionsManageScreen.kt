package com.movtery.zalithlauncher.ui.screens.content

import android.os.Environment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.path.GamePathManager
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.ui.activities.MainActivity
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.screens.content.download.DOWNLOAD_GAME_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.elements.GamePathItemLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.GamePathOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionItemLayout
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionsOperation
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.navigateToDownload
import com.movtery.zalithlauncher.ui.screens.navigateToFileSelector
import com.movtery.zalithlauncher.utils.StoragePermissionsUtils
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState

const val VERSIONS_MANAGE_SCREEN_TAG = "VersionsManageScreen"

@Composable
fun VersionsManageScreen(
    navController: NavController
) {
    BaseScreen(
        screenTag = VERSIONS_MANAGE_SCREEN_TAG,
        currentTag = MutableStates.mainScreenTag
    ) { isVisible ->
        Row {
            GamePathLayout(
                isVisible = isVisible,
                navController = navController,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(2.5f)
            )

            VersionsLayout(
                isVisible = isVisible,
                navController = navController,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(7.5f)
                    .padding(vertical = 12.dp)
                    .padding(end = 12.dp),
                onRefresh = {
                    if (!VersionsManager.isRefreshing) {
                        VersionsManager.refresh()
                    }
                },
                onInstall = {
                    navController.navigateToDownload(DOWNLOAD_GAME_SCREEN_TAG)
                }
            )
        }
    }
}

@Composable
private fun GamePathLayout(
    isVisible: Boolean,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val surfaceXOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    var gamePathOperation by remember { mutableStateOf<GamePathOperation>(GamePathOperation.None) }
    MutableStates.filePathSelector?.let {
        if (it.saveTag == VERSIONS_MANAGE_SCREEN_TAG) {
            gamePathOperation = GamePathOperation.AddNewPath(it.path)
            MutableStates.filePathSelector = null
        }
    }
    GamePathOperation(
        gamePathOperation = gamePathOperation,
        changeState = { gamePathOperation = it }
    )

    Column(
        modifier = modifier.offset { IntOffset(x = surfaceXOffset.roundToPx(), y = 0) },
    ) {
        val gamePaths by GamePathManager.gamePathData.collectAsState()
        val currentPath = GamePathManager.currentPath
        val context = LocalContext.current

        LazyColumn(
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(gamePaths) { pathItem ->
                GamePathItemLayout(
                    item = pathItem,
                    selected = currentPath == pathItem.path,
                    onClick = {
                        if (!VersionsManager.isRefreshing) { //避免频繁刷新，防止currentGameInfo意外重置
                            if (pathItem.id == GamePathManager.DEFAULT_ID) {
                                GamePathManager.saveDefaultPath()
                            } else {
                                (context as? MainActivity)?.let { activity ->
                                    StoragePermissionsUtils.checkPermissions(activity = activity, hasPermission = {
                                        GamePathManager.saveCurrentPath(pathItem.id)
                                    })
                                }
                            }
                        }
                    },
                    onDelete = {
                        gamePathOperation = GamePathOperation.DeletePath(pathItem)
                    },
                    onRename = {
                        gamePathOperation = GamePathOperation.RenamePath(pathItem)
                    }
                )
            }
        }

        ScalingActionButton(
            modifier = Modifier
                .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))
                .fillMaxWidth(),
            onClick = {
                (context as? MainActivity)?.let { activity ->
                    StoragePermissionsUtils.checkPermissions(activity = activity, hasPermission = {
                        navController.navigateToFileSelector(
                            startPath = Environment.getExternalStorageDirectory().absolutePath,
                            selectFile = false,
                            saveTag = VERSIONS_MANAGE_SCREEN_TAG
                        )
                    })
                }
            }
        ) {
            Text(text = stringResource(R.string.versions_manage_game_path_add_new))
        }
    }
}

@Composable
private fun VersionsLayout(
    isVisible: Boolean,
    navController: NavController,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit,
    onInstall: () -> Unit
) {
    val surfaceYOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible
    )

    Card(
        modifier = modifier
            .offset {
                IntOffset(
                    x = 0,
                    y = surfaceYOffset.roundToPx()
                )
            },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        if (VersionsManager.isRefreshing) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            val versions by VersionsManager.versions.collectAsState()

            if (versions.isNotEmpty()) {
                VersionsManager.currentVersion?.let { currentVersion ->
                    var versionsOperation by remember { mutableStateOf<VersionsOperation>(VersionsOperation.None) }
                    VersionsOperation(versionsOperation) { versionsOperation = it }

                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxWidth().padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))) {
                            Row(modifier = Modifier.padding(horizontal = 4.dp).fillMaxWidth()) {
                                IconTextButton(
                                    onClick = onRefresh,
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.generic_refresh),
                                    text = stringResource(R.string.generic_refresh),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconTextButton(
                                    onClick = onInstall,
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = stringResource(R.string.versions_manage_install_new),
                                    text = stringResource(R.string.versions_manage_install_new),
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            items(versions) { version ->
                                VersionItemLayout(
                                    version = version,
                                    selected = version == currentVersion,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    onSelected = {
                                        if (version.isValid()) {
                                            if (version != currentVersion) {
                                                VersionsManager.saveCurrentVersion(version.getVersionName())
                                            }
                                        } else {
                                            //不允许选择无效版本
                                            versionsOperation = VersionsOperation.InvalidDelete(version)
                                        }
                                    },
                                    onSettingsClick = {
                                        VersionsManager.versionBeingSet = version
                                        navController.navigateTo(VERSION_SETTINGS_SCREEN_TAG)
                                    },
                                    onRenameClick = { versionsOperation = VersionsOperation.Rename(version) },
                                    onCopyClick = { versionsOperation = VersionsOperation.Copy(version) },
                                    onDeleteClick = { versionsOperation = VersionsOperation.Delete(version) }
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ScalingLabel(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(R.string.versions_manage_no_versions),
                        onClick = {
                            navController.navigateToDownload()
                        }
                    )
                }
            }
        }
    }
}
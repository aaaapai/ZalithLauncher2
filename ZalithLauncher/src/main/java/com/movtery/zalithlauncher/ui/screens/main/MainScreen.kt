package com.movtery.zalithlauncher.ui.screens.main

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.automirrored.rounded.ArrowLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.clearWith
import com.movtery.zalithlauncher.ui.screens.content.AccountManageScreen
import com.movtery.zalithlauncher.ui.screens.content.DownloadScreen
import com.movtery.zalithlauncher.ui.screens.content.FileSelectorScreen
import com.movtery.zalithlauncher.ui.screens.content.LauncherScreen
import com.movtery.zalithlauncher.ui.screens.content.LicenseScreen
import com.movtery.zalithlauncher.ui.screens.content.SettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionSettingsScreen
import com.movtery.zalithlauncher.ui.screens.content.VersionsManageScreen
import com.movtery.zalithlauncher.ui.screens.content.WebViewScreen
import com.movtery.zalithlauncher.ui.screens.content.navigateToDownload
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.LaunchGameViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

@Composable
fun MainScreen(
    screenBackStackModel: ScreenBackStackViewModel,
    launchGameViewModel: LaunchGameViewModel,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxHeight()
    ) {
        val tasks by TaskSystem.tasksFlow.collectAsState()

        val isTaskMenuExpanded = AllSettings.launcherTaskMenuExpanded.state

        fun changeTasksExpandedState() {
            AllSettings.launcherTaskMenuExpanded.save(!isTaskMenuExpanded)
        }

        /** 回到主页面通用函数 */
        val toMainScreen: () -> Unit = {
            screenBackStackModel.mainScreenBackStack.clearWith(NormalNavKey.LauncherMain)
        }

        TopBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .zIndex(10f),
            mainScreenKey = screenBackStackModel.mainScreenKey,
            taskRunning = tasks.isEmpty(),
            isTasksExpanded = isTaskMenuExpanded,
            color = MaterialTheme.colorScheme.surfaceContainer,
            onScreenBack = {
                screenBackStackModel.mainScreenBackStack.removeFirstOrNull()
            },
            toMainScreen = toMainScreen,
            toSettingsScreen = {
                screenBackStackModel.mainScreenBackStack.navigateTo(
                    NestedNavKey.Settings(
                        screenBackStackModel.settingsBackStack
                    )
                )
            },
            toDownloadScreen = {
                screenBackStackModel.navigateToDownload()
            }
        ) {
            changeTasksExpandedState()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            NavigationUI(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.surface),
                screenBackStackModel = screenBackStackModel,
                toMainScreen = toMainScreen,
                launchGameViewModel = launchGameViewModel,
                summitError = summitError
            )

            TaskMenu(
                tasks = tasks,
                isExpanded = isTaskMenuExpanded,
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .padding(all = 6.dp)
            ) {
                changeTasksExpandedState()
            }
        }
    }
}

@Composable
private fun TopBar(
    mainScreenKey: NavKey?,
    taskRunning: Boolean,
    isTasksExpanded: Boolean,
    modifier: Modifier = Modifier,
    color: Color,
    onScreenBack: () -> Unit,
    toMainScreen: () -> Unit,
    toSettingsScreen: () -> Unit,
    toDownloadScreen: () -> Unit,
    changeExpandedState: () -> Unit = {}
) {
    val inLauncherScreen = mainScreenKey == null || mainScreenKey is NormalNavKey.LauncherMain
    val inDownloadScreen = mainScreenKey is NestedNavKey.Download

    Surface(
        modifier = modifier,
        color = color,
        tonalElevation = 3.dp
    ) {
        ConstraintLayout {
            val (backButton, title, endButtons) = createRefs()

            val backButtonX by animateDpAsState(
                targetValue = if (inLauncherScreen) -(60).dp else 0.dp,
                animationSpec = getAnimateTween()
            )
            val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

            IconButton(
                modifier = Modifier
                    .offset { IntOffset(x = backButtonX.roundToPx(), y = 0) }
                    .constrainAs(backButton) {
                        start.linkTo(parent.start, margin = 12.dp)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                    }
                    .fillMaxHeight(),
                onClick = {
                    if (!inLauncherScreen) {
                        //不在主屏幕时才允许返回
                        backDispatcher?.onBackPressed() ?: run {
                            onScreenBack()
                        }
                    }
                }
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
                    contentDescription = stringResource(R.string.generic_back)
                )
            }

            val appTitleX by animateDpAsState(
                targetValue = if (inLauncherScreen) 0.dp else 48.dp,
                animationSpec = getAnimateTween()
            )

            Text(
                text = InfoDistributor.LAUNCHER_IDENTIFIER,
                modifier = Modifier
                    .offset { IntOffset(x = appTitleX.roundToPx(), y = 0) }
                    .constrainAs(title) {
                        centerVerticallyTo(parent)
                        start.linkTo(parent.start, margin = 18.dp)
                    }
            )

            Row(
                modifier = Modifier
                    .constrainAs(endButtons) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        end.linkTo(parent.end, margin = 12.dp)
                    }
            ) {
                val taskLayoutY by animateDpAsState(
                    targetValue = if (isTasksExpanded || taskRunning) (-50).dp else 0.dp,
                    animationSpec = getAnimateTween()
                )

                Row(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = taskLayoutY.roundToPx()) }
                        .clip(shape = MaterialTheme.shapes.large)
                        .clickable { changeExpandedState() }
                        .padding(all = 8.dp)
                        .width(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically))
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = Icons.Filled.Task,
                        contentDescription = stringResource(R.string.main_task_menu)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                AnimatedVisibility(
                    visible = !inDownloadScreen,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    IconButton(
                        onClick = toDownloadScreen
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = stringResource(R.string.generic_download)
                        )
                    }
                }

                IconButton(
                    modifier = Modifier.fillMaxHeight(),
                    onClick = {
                        if (inLauncherScreen) {
                            toSettingsScreen()
                        } else {
                            toMainScreen()
                        }
                    }
                ) {
                    Crossfade(
                        targetState = inLauncherScreen,
                        label = "SettingsIconCrossfade",
                        animationSpec = getAnimateTween()
                    ) { isLauncherScreen ->
                        Icon(
                            imageVector = if (isLauncherScreen) {
                                Icons.Filled.Settings
                            } else {
                                Icons.Filled.Home
                            },
                            contentDescription = if (isLauncherScreen) {
                                stringResource(R.string.generic_setting)
                            } else {
                                stringResource(R.string.generic_main_menu)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationUI(
    modifier: Modifier = Modifier,
    screenBackStackModel: ScreenBackStackViewModel,
    toMainScreen: () -> Unit,
    launchGameViewModel: LaunchGameViewModel,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val backStack = screenBackStackModel.mainScreenBackStack
    val currentKey = backStack.lastOrNull()

    LaunchedEffect(currentKey) {
        screenBackStackModel.mainScreenKey = currentKey
    }

    if (backStack.isNotEmpty()) {
        /** 导航至版本详细信息屏幕 */
        val navigateToVersions: (Version) -> Unit = { version ->
            screenBackStackModel.versionsBackStack.clearWith(NormalNavKey.Versions.OverView)
            screenBackStackModel.mainScreenBackStack.navigateTo(
                screenKey = NestedNavKey.Versions(
                    backStack = screenBackStackModel.versionsBackStack,
                    version = version
                ),
                useClassEquality = true
            )
        }

        NavDisplay(
            backStack = backStack,
            modifier = modifier,
            onBack = {
                onBack(backStack)
            },
            entryProvider = entryProvider {
                entry<NormalNavKey.LauncherMain> {
                    LauncherScreen(
                        backStackViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        launchGameViewModel = launchGameViewModel
                    )
                }
                entry<NestedNavKey.Settings> { key ->
                    SettingsScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel,
                        openLicenseScreen = { raw ->
                            backStack.navigateTo(NormalNavKey.License(raw))
                        },
                        summitError = summitError
                    )
                }
                entry<NormalNavKey.License> { key ->
                    LicenseScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel
                    )
                }
                entry<NormalNavKey.AccountManager> {
                    AccountManageScreen(
                        backStackViewModel = screenBackStackModel,
                        backToMainScreen = {
                            screenBackStackModel.mainScreenBackStack.clearWith(NormalNavKey.LauncherMain)
                        },
                        summitError = summitError
                    )
                }
                entry<NormalNavKey.WebScreen> { key ->
                    WebViewScreen(
                        key = key,
                        backStackViewModel = screenBackStackModel
                    )
                }
                entry<NormalNavKey.VersionsManager> {
                    VersionsManageScreen(
                        backScreenViewModel = screenBackStackModel,
                        navigateToVersions = navigateToVersions,
                        summitError = summitError
                    )
                }
                entry<NormalNavKey.FileSelector> { key ->
                    FileSelectorScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel
                    ) {
                        backStack.removeLastOrNull()
                    }
                }
                entry<NestedNavKey.Versions> { key ->
                    VersionSettingsScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        backToMainScreen = toMainScreen,
                        launchGameViewModel = launchGameViewModel,
                        summitError = summitError
                    )
                }
                entry<NestedNavKey.Download> { key ->
                    DownloadScreen(
                        key = key,
                        backScreenViewModel = screenBackStackModel,
                        summitError = summitError
                    )
                }
            }
        )
    } else {
        Box(modifier)
    }
}

@Composable
private fun TaskMenu(
    tasks: List<Task>,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    changeExpandedState: () -> Unit = {}
) {
    val show = isExpanded && tasks.isNotEmpty()
    val surfaceX by animateDpAsState(
        targetValue = if (show) 0.dp else (-260).dp,
        animationSpec = getAnimateTween()
    )
    val surfaceAlpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = getAnimateTween()
    )

    Card(
        modifier = modifier
            .offset { IntOffset(x = surfaceX.roundToPx(), y = 0) }
            .alpha(surfaceAlpha)
            .padding(all = 6.dp)
            .width(240.dp),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                IconButton(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterStart),
                    onClick = changeExpandedState
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowLeft,
                        contentDescription = stringResource(R.string.generic_collapse)
                    )
                }

                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(R.string.main_task_menu)
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                items(tasks) { task ->
                    TaskItem(
                        taskProgress = task.currentProgress,
                        taskMessageRes = task.currentMessageRes,
                        taskMessageArgs = task.currentMessageArgs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        //取消任务
                        TaskSystem.cancelTask(task.id)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    taskProgress: Float,
    taskMessageRes: Int?,
    taskMessageArgs: Array<out Any>?,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onCancelClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterVertically),
                onClick = onCancelClick
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.generic_cancel)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
                    .animateContentSize(animationSpec = getAnimateTween())
            ) {
                taskMessageRes?.let { messageRes ->
                    Text(
                        text = if (taskMessageArgs != null) {
                            stringResource(messageRes, *taskMessageArgs)
                        } else {
                            stringResource(messageRes)
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                if (taskProgress < 0) { //负数则代表不确定
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { taskProgress },
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                        )
                        Text(
                            text = "${(taskProgress * 100).toInt()}%",
                            modifier = Modifier.align(Alignment.CenterVertically),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
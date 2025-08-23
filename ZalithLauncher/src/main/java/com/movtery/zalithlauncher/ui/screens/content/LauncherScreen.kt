package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.movtery.zalithlauncher.BuildConfig
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.AccountAvatar
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import com.movtery.zalithlauncher.ui.screens.content.elements.getLocalSkinWarningButton
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.viewmodel.LaunchGameViewModel
import com.movtery.zalithlauncher.viewmodel.ScreenBackStackViewModel

@Composable
fun LauncherScreen(
    backStackViewModel: ScreenBackStackViewModel,
    navigateToVersions: (Version) -> Unit,
    launchGameViewModel: LaunchGameViewModel
) {
    BaseScreen(
        screenKey = NormalNavKey.LauncherMain,
        currentKey = backStackViewModel.mainScreenKey
    ) { isVisible ->
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            ContentMenu(
                isVisible = isVisible,
                modifier = Modifier.weight(7f)
            )

            RightMenu(
                isVisible = isVisible,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
                launchGameViewModel = launchGameViewModel,
                toAccountManageScreen = {
                    backStackViewModel.mainScreenBackStack.navigateTo(NormalNavKey.AccountManager)
                },
                toVersionManageScreen = {
                    backStackViewModel.mainScreenBackStack.navigateTo(NormalNavKey.VersionsManager)
                },
                toVersionSettingsScreen = {
                    VersionsManager.currentVersion?.let { version ->
                        navigateToVersions(version)
                    }
                },
                toDownloadScreen = { projectId, platform, classes ->
                    backStackViewModel.navigateToDownload(
                        targetScreen = NestedNavKey.DownloadMod(
                            backStack = backStackViewModel.downloadModBackStack.also { stack ->
                                stack.navigateTo(
                                    NormalNavKey.DownloadAssets(platform, projectId, classes)
                                )
                            }
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun ContentMenu(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    val yOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .offset { IntOffset(x = 0, y = yOffset.roundToPx()) }
    ) {
        if (BuildConfig.DEBUG) {
            //debug版本关不掉的警告，防止有人把测试版当正式版用 XD
            Card(
                modifier = Modifier.padding(all = 12.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.generic_warning),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.launcher_version_debug_warning, InfoDistributor.LAUNCHER_NAME),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        modifier = Modifier
                            .alpha(0.8f)
                            .align(Alignment.End),
                        text = stringResource(R.string.launcher_version_debug_warning_cant_close),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun RightMenu(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    launchGameViewModel: LaunchGameViewModel,
    toAccountManageScreen: () -> Unit = {},
    toVersionManageScreen: () -> Unit = {},
    toVersionSettingsScreen: () -> Unit = {},
    toDownloadScreen: (id: String, Platform, classes: PlatformClasses) -> Unit = { _, _, _ -> }
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = 40.dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    Card(
        modifier = modifier.offset { IntOffset(x = xOffset.roundToPx(), y = 0) },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        val account by AccountsManager.currentAccountFlow.collectAsState()
        val version = VersionsManager.currentVersion

        ConstraintLayout(
            modifier = Modifier.fillMaxSize()
        ) {
            val (accountAvatar, skinWarningLayout, versionManagerLayout, launchButton) = createRefs()

            AccountAvatar(
                modifier = Modifier
                    .constrainAs(accountAvatar) {
                        top.linkTo(parent.top)
                        bottom.linkTo(launchButton.top, margin = 32.dp)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                account = account,
                onClick = toAccountManageScreen
            )

            val skinWarning: (@Composable () -> Unit)? = version?.getVersionInfo()?.let { versionInfo ->
                account?.let { acc1 ->
                    //离线账号皮肤相关的警告
                    getLocalSkinWarningButton(
                        account = acc1,
                        versionInfo = versionInfo,
                        swapToAccountScreen = toAccountManageScreen,
                        swapToDownloadScreen = toDownloadScreen
                    )
                }
            }

            skinWarning?.let { button ->
                Column(
                    modifier = Modifier
                        .constrainAs(skinWarningLayout) {
                            top.linkTo(accountAvatar.top, margin = 50.dp)
                            start.linkTo(accountAvatar.start, margin = 50.dp)
                        }
                ) {
                    button()
                }
            }

            Row(
                modifier = Modifier.constrainAs(versionManagerLayout) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(launchButton.top)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                VersionManagerLayout(
                    version = version,
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f)
                        .padding(8.dp),
                    swapToVersionManage = toVersionManageScreen
                )
                version?.takeIf { it.isValid() }?.let {
                    IconButton(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = toVersionSettingsScreen
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.versions_manage_settings)
                        )
                    }
                }
            }

            ScalingActionButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .constrainAs(launchButton) {
                        bottom.linkTo(parent.bottom, margin = 8.dp)
                    }
                    .padding(PaddingValues(horizontal = 12.dp)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                onClick = {
                    launchGameViewModel.tryLaunch(
                        VersionsManager.currentVersion
                    )
                },
            ) {
                MarqueeText(text = stringResource(R.string.main_launch_game))
            }
        }
    }
}

@Composable
private fun VersionManagerLayout(
    version: Version?,
    modifier: Modifier = Modifier,
    swapToVersionManage: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(onClick = swapToVersionManage)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 8.dp, vertical = 4.dp))
        ) {
            if (VersionsManager.isRefreshing) {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center))
                }
            } else {
                VersionIconImage(
                    version = version,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(8.dp))

                if (version == null) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .basicMarquee(iterations = Int.MAX_VALUE),
                        text = stringResource(R.string.versions_manage_no_versions),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically)
                    ) {
                        Text(
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            text = version.getVersionName(),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1
                        )
                        if (version.isValid()) {
                            Text(
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                                text = version.getVersionSummary(),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
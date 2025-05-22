package com.movtery.zalithlauncher.ui.screens.content

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.screens.content.elements.AccountAvatar
import com.movtery.zalithlauncher.ui.screens.content.elements.LaunchGameOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage
import com.movtery.zalithlauncher.ui.screens.content.elements.getLocalSkinWarningButton
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState

const val LAUNCHER_SCREEN_TAG: String = "LauncherScreen"

@Composable
fun LauncherScreen(
    navController: NavController
) {
    BaseScreen(
        screenTag = LAUNCHER_SCREEN_TAG,
        currentTag = MutableStates.mainScreenTag
    ) { isVisible ->
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(7f))
            RightMenu(
                isVisible = isVisible,
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
                navController = navController
            )
        }
    }
}

@Composable
private fun RightMenu(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = 40.dp,
        swapIn = isVisible,
        isHorizontal = true
    )

    var launchGameOperation by remember { mutableStateOf<LaunchGameOperation>(LaunchGameOperation.None) }
    LaunchGameOperation(
        launchGameOperation = launchGameOperation,
        updateOperation = { launchGameOperation = it },
        navController = navController
    )

    Card(
        modifier = modifier.offset {
            IntOffset(
                x = xOffset.roundToPx(),
                y = 0
            )
        },
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
                account = account
            ) {
                navController.navigateTo(ACCOUNT_MANAGE_SCREEN_TAG)
            }

            val skinWarning: (@Composable () -> Unit)? = version?.getVersionInfo()?.let { versionInfo ->
                account?.let { acc1 ->
                    //离线账号皮肤相关的警告
                    getLocalSkinWarningButton(
                        account = acc1,
                        versionInfo = versionInfo,
                        swapToAccountScreen = {
                            navController.navigateTo(ACCOUNT_MANAGE_SCREEN_TAG)
                        }
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
                    swapToVersionManage = {
                        navController.navigateTo(VERSIONS_MANAGE_SCREEN_TAG)
                    }
                )
                version?.takeIf { it.isValid() }?.let {
                    IconButton(
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = {
                            VersionsManager.versionBeingSet = VersionsManager.currentVersion
                            navController.navigateTo(VERSION_SETTINGS_SCREEN_TAG)
                        }
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
                    launchGameOperation = LaunchGameOperation.TryLaunch
                },
            ) {
                Text(text = stringResource(R.string.main_launch_game))
            }
        }
    }
}

@Composable
private fun VersionManagerLayout(
    version: Version?,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                        overflow = TextOverflow.Clip,
                        text = stringResource(R.string.versions_manage_no_versions),
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
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
                            overflow = TextOverflow.Clip,
                            text = version.getVersionName(),
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor,
                            maxLines = 1
                        )
                        if (version.isValid()) {
                            Text(
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                                overflow = TextOverflow.Clip,
                                text = version.getVersionSummary(),
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
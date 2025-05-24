package com.movtery.zalithlauncher.ui.screens.content.elements

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.launch.LaunchGame
import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.skin.SkinModelType
import com.movtery.zalithlauncher.game.skin.isOfflineSkinCompatible
import com.movtery.zalithlauncher.game.skin.isSkinModelUuidSupported
import com.movtery.zalithlauncher.game.version.installed.VersionInfo
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.TooltipIconButton
import com.movtery.zalithlauncher.ui.screens.content.ACCOUNT_MANAGE_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.VERSIONS_MANAGE_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import com.movtery.zalithlauncher.utils.string.isBiggerTo
import com.movtery.zalithlauncher.utils.string.isLowerTo
import kotlinx.coroutines.launch

sealed interface LaunchGameOperation {
    data object None : LaunchGameOperation
    /** 没有安装版本/没有选中有效版本 */
    data object NoVersion : LaunchGameOperation
    /** 没有可用账号 */
    data object NoAccount : LaunchGameOperation
    /** 当前渲染器不支持选中版本 */
    data class UnsupportedRenderer(val renderer: RendererInterface): LaunchGameOperation
    /** 尝试启动：启动前检查一些东西 */
    data object TryLaunch : LaunchGameOperation
    /** 正式启动 */
    data object RealLaunch : LaunchGameOperation
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun getLocalSkinWarningButton(
    modifier: Modifier = Modifier,
    account: Account,
    versionInfo: VersionInfo,
    swapToAccountScreen: () -> Unit = {}
): (@Composable () -> Unit)? {
    val context = LocalContext.current

    val warningIcon = @Composable {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = stringResource(R.string.generic_warning),
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        )
    }

    if (!(account.isLocalAccount() && account.hasSkinFile)) return null

    return if (
        account.skinModelType == SkinModelType.ALEX &&
        isSkinModelUuidSupported(versionInfo)
    ) {
        @Composable {
            TooltipIconButton(
                modifier = modifier,
                tooltipTitle = stringResource(R.string.generic_warning),
                tooltipMessage = stringResource(R.string.account_change_skin_not_supported_alex)
            ) {
                warningIcon()
            }
        }
    } else if (isOfflineSkinCompatible(versionInfo)) {
        @Composable {
            val tooltipState = rememberTooltipState(isPersistent = true)
            val coroutineScope = rememberCoroutineScope()

            TooltipBox(
                modifier = modifier,
                positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
                tooltip = {
                    RichTooltip(
                        modifier = Modifier.padding(all = 3.dp),
                        title = { Text(text = stringResource(R.string.generic_warning)) },
                        shadowElevation = 3.dp
                    ) {
                        Column {
                            Text(text = stringResource(R.string.account_change_skin_compatibility_warning))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.account_change_skin_select_model_alert_hint5),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            IconTextButton(
                                onClick = {
                                    swapToAccountScreen()
                                    tooltipState.dismiss()
                                },
                                imageVector = Icons.Outlined.Dns,
                                contentDescription = null,
                                text = stringResource(R.string.account_change_skin_select_model_alert_auth_server)
                            )
                            IconTextButton(
                                onClick = {
                                    NetWorkUtils.openLink(context, context.getString(R.string.url_mod_custom_skin_loader))
                                    tooltipState.dismiss()
                                },
                                imageVector = Icons.Outlined.Checkroom,
                                contentDescription = null,
                                text = stringResource(R.string.account_change_skin_select_model_alert_custom_skin_loader)
                            )
                        }
                    }
                },
                state = tooltipState,
                enableUserInput = false
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (tooltipState.isVisible) {
                                tooltipState.dismiss()
                            } else {
                                tooltipState.show()
                            }
                        }
                    }
                ) {
                    warningIcon()
                }
            }
        }
    } else null
}

@Composable
fun LaunchGameOperation(
    launchGameOperation: LaunchGameOperation,
    updateOperation: (LaunchGameOperation) -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    when (launchGameOperation) {
        is LaunchGameOperation.None -> {}
        is LaunchGameOperation.NoVersion -> {
            Toast.makeText(context, R.string.game_launch_no_version, Toast.LENGTH_SHORT).show()
            navController.navigateTo(VERSIONS_MANAGE_SCREEN_TAG)
            updateOperation(LaunchGameOperation.None)
        }
        is LaunchGameOperation.NoAccount -> {
            Toast.makeText(context, R.string.game_launch_no_account, Toast.LENGTH_SHORT).show()
            navController.navigateTo(ACCOUNT_MANAGE_SCREEN_TAG)
            updateOperation(LaunchGameOperation.None)
        }
        is LaunchGameOperation.UnsupportedRenderer -> {
            val renderer = launchGameOperation.renderer
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.renderer_version_unsupported_warning, renderer.getRendererName()),
                confirmText = stringResource(R.string.generic_anyway),
                onConfirm = {
                    updateOperation(LaunchGameOperation.RealLaunch)
                },
                onDismiss = {
                    updateOperation(LaunchGameOperation.None)
                }
            )
        }
        is LaunchGameOperation.TryLaunch -> {
            val version = VersionsManager.currentVersion ?: run {
                updateOperation(LaunchGameOperation.NoVersion)
                return
            }

            AccountsManager.getCurrentAccount() ?: run {
                updateOperation(LaunchGameOperation.NoAccount)
                return
            }

            //开始检查渲染器的版本支持情况
            Renderers.setCurrentRenderer(context, version.getRenderer())
            val currentRenderer = Renderers.getCurrentRenderer()
            val rendererMinVer = currentRenderer.getMinMCVersion()
            val rendererMaxVer = currentRenderer.getMaxMCVersion()

            val mcVer = version.getVersionInfo()!!.minecraftVersion

            val isUnsupported =
                (rendererMinVer?.let { mcVer.isLowerTo(it) } ?: false) ||
                (rendererMaxVer?.let { mcVer.isBiggerTo(it) } ?: false)

            if (isUnsupported) {
                updateOperation(LaunchGameOperation.UnsupportedRenderer(currentRenderer))
                return
            }

            //正式启动游戏
            updateOperation(LaunchGameOperation.RealLaunch)
        }
        is LaunchGameOperation.RealLaunch -> {
            val version = VersionsManager.currentVersion ?: run {
                updateOperation(LaunchGameOperation.None)
                return
            }
            version.apply {
                offlineAccountLogin = false
                quickPlaySingle = null //清除快速启动
            }
            LaunchGame.launchGame(context, version)
            updateOperation(LaunchGameOperation.None)
        }
    }
}
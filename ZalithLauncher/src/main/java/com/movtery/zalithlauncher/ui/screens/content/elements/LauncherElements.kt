package com.movtery.zalithlauncher.ui.screens.content.elements

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.launch.LaunchGame
import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.screens.content.ACCOUNT_MANAGE_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.VERSIONS_MANAGE_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.utils.string.isBiggerTo
import com.movtery.zalithlauncher.utils.string.isLowerTo

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
            LaunchGame.launchGame(context)
            updateOperation(LaunchGameOperation.None)
        }
    }
}
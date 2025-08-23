package com.movtery.zalithlauncher.ui.screens.content.download

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.google.gson.JsonSyntaxException
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.game.GameDownloadInfo
import com.movtery.zalithlauncher.game.download.game.GameInstaller
import com.movtery.zalithlauncher.game.download.game.optifine.CantFetchingOptiFineUrlException
import com.movtery.zalithlauncher.game.download.jvm_server.JvmCrashException
import com.movtery.zalithlauncher.game.version.download.DownloadFailedException
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.notification.NotificationManager
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.NotificationCheck
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.common.GameInstallOperation
import com.movtery.zalithlauncher.ui.screens.content.download.common.GameInstallingDialog
import com.movtery.zalithlauncher.ui.screens.content.download.game.DownloadGameWithAddonScreen
import com.movtery.zalithlauncher.ui.screens.content.download.game.SelectGameVersionScreen
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

private class GameDownloadViewModel(): ViewModel() {
    var installOperation by mutableStateOf<GameInstallOperation>(GameInstallOperation.None)

    /**
     * 游戏安装器
     */
    var installer by mutableStateOf<GameInstaller?>(null)

    fun install(
        context: Context,
        info: GameDownloadInfo
    ) {
        installer = GameInstaller(context, info, viewModelScope).also {
            it.installGame(
                onInstalled = {
                    installer = null
                    VersionsManager.refresh()
                    installOperation = GameInstallOperation.Success
                },
                onError = { th ->
                    installer = null
                    installOperation = GameInstallOperation.Error(th)
                }
            )
        }
    }

    fun cancel() {
        installer?.cancelInstall()
        installer = null
    }

    override fun onCleared() {
        cancel()
    }
}

@Composable
private fun rememberGameDownloadViewModel(
    key: NestedNavKey.DownloadGame
): GameDownloadViewModel {
    return viewModel(
        key = key.toString()
    ) {
        GameDownloadViewModel()
    }
}

@Composable
fun DownloadGameScreen(
    key: NestedNavKey.DownloadGame,
    mainScreenKey: NavKey?,
    downloadScreenKey: NavKey?,
    downloadGameScreenKey: NavKey?,
    onCurrentKeyChange: (NavKey?) -> Unit,
) {
    val viewModel: GameDownloadViewModel = rememberGameDownloadViewModel(key)

    val context = LocalContext.current
    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        onCurrentKeyChange(stackTopKey)
    }

    GameInstallOperation(
        gameInstallOperation = viewModel.installOperation,
        updateOperation = { viewModel.installOperation = it },
        installer = viewModel.installer,
        onInstall = { info ->
            viewModel.install(context, info)
        },
        onCancel = {
            viewModel.cancel()
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
                entry<NormalNavKey.DownloadGame.SelectGameVersion> {
                    SelectGameVersionScreen(
                        mainScreenKey = mainScreenKey,
                        downloadScreenKey = downloadScreenKey,
                        downloadGameScreenKey = downloadGameScreenKey
                    ) { versionString ->
                        backStack.navigateTo(
                            NormalNavKey.DownloadGame.Addons(versionString)
                        )
                    }
                }
                entry<NormalNavKey.DownloadGame.Addons> { key ->
                    val context = LocalContext.current
                    DownloadGameWithAddonScreen(
                        mainScreenKey = mainScreenKey,
                        downloadScreenKey = downloadScreenKey,
                        downloadGameScreenKey = downloadGameScreenKey,
                        key = key
                    ) { info ->
                        if (viewModel.installOperation !is GameInstallOperation.None) {
                            //不是待安装状态，拒绝此次安装
                            return@DownloadGameWithAddonScreen
                        }
                        viewModel.installOperation = if (!NotificationManager.checkNotificationEnabled(context)) {
                            //警告通知权限
                            GameInstallOperation.WarningForNotification(info)
                        } else {
                            GameInstallOperation.Install(info)
                        }
                    }
                }
            }
        )
    } else {
        Box(Modifier.fillMaxSize())
    }
}

@Composable
private fun GameInstallOperation(
    gameInstallOperation: GameInstallOperation,
    updateOperation: (GameInstallOperation) -> Unit = {},
    installer: GameInstaller?,
    onInstall: (GameDownloadInfo) -> Unit,
    onCancel: () -> Unit
) {
    when (gameInstallOperation) {
        is GameInstallOperation.None -> {}
        is GameInstallOperation.WarningForNotification -> {
            NotificationCheck(
                onGranted = {
                    //权限被授予，开始安装
                    updateOperation(GameInstallOperation.Install(gameInstallOperation.info))
                },
                onIgnore = {
                    //用户不想授权，但是支持继续进行安装
                    updateOperation(GameInstallOperation.Install(gameInstallOperation.info))
                },
                onDismiss = {
                    updateOperation(GameInstallOperation.None)
                }
            )
        }
        is GameInstallOperation.Install -> {
            if (installer != null) {
                val installGame = installer.tasksFlow.collectAsState()
                if (installGame.value.isNotEmpty()) {
                    //安装游戏的弹窗
                    GameInstallingDialog(
                        title = stringResource(R.string.download_game_install_title),
                        tasks = installGame.value,
                        onCancel = {
                            onCancel()
                            updateOperation(GameInstallOperation.None)
                        }
                    )
                }
            } else {
                onInstall(gameInstallOperation.info)
            }
        }
        is GameInstallOperation.Error -> {
            val th = gameInstallOperation.th
            lError("Failed to download the game!", th)
            val message = when (th) {
                is HttpRequestTimeoutException, is SocketTimeoutException -> stringResource(R.string.error_timeout)
                is UnknownHostException, is UnresolvedAddressException -> stringResource(R.string.error_network_unreachable)
                is ConnectException -> stringResource(R.string.error_connection_failed)
                is SerializationException, is JsonSyntaxException -> stringResource(R.string.error_parse_failed)
                is CantFetchingOptiFineUrlException -> stringResource(R.string.download_install_error_cant_fetch_optifine_download_url)
                is JvmCrashException -> stringResource(R.string.download_install_error_jvm_crash, th.code)
                is DownloadFailedException -> stringResource(R.string.download_install_error_download_failed)
                else -> {
                    val errorMessage = th.localizedMessage ?: th.message ?: th::class.qualifiedName ?: "Unknown error"
                    stringResource(R.string.error_unknown, errorMessage)
                }
            }
            val dismiss = {
                updateOperation(GameInstallOperation.None)
            }
            AlertDialog(
                onDismissRequest = dismiss,
                title = {
                    Text(text = stringResource(R.string.download_install_error_title))
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = stringResource(R.string.download_install_error_message))
                        Text(text = message)
                    }
                },
                confirmButton = {
                    Button(onClick = dismiss) {
                        MarqueeText(text = stringResource(R.string.generic_confirm))
                    }
                }
            )
        }
        is GameInstallOperation.Success -> {
            SimpleAlertDialog(
                title = stringResource(R.string.download_install_success_title),
                text = stringResource(R.string.download_install_success_message)
            ) {
                updateOperation(GameInstallOperation.None)
            }
        }
    }
}
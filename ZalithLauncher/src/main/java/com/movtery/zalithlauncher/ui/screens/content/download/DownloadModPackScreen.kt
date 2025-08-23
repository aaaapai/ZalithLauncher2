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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.jvm_server.JvmCrashException
import com.movtery.zalithlauncher.game.download.modpack.install.ModPackInfo
import com.movtery.zalithlauncher.game.download.modpack.install.ModPackInstaller
import com.movtery.zalithlauncher.game.version.download.DownloadFailedException
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.notification.NotificationManager
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.NotificationCheck
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.download.DownloadAssetsScreen
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.DownloadVersionInfo
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchModPackScreen
import com.movtery.zalithlauncher.ui.screens.content.download.common.GameInstallingDialog
import com.movtery.zalithlauncher.ui.screens.content.download.common.ModPackInstallOperation
import com.movtery.zalithlauncher.ui.screens.content.download.common.VersionNameOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.isFilenameInvalid
import com.movtery.zalithlauncher.ui.screens.navigateTo
import com.movtery.zalithlauncher.ui.screens.onBack
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private class ModPackViewModel: ViewModel() {
    var installOperation by mutableStateOf<ModPackInstallOperation>(ModPackInstallOperation.None)
    var versionNameOperation by mutableStateOf<VersionNameOperation>(VersionNameOperation.None)

    //等待用户输入版本名称相关
    private var versionNameContinuation: (Continuation<String>)? = null
    suspend fun waitForVersionName(modPackInfo: ModPackInfo): String {
        return suspendCancellableCoroutine { cont ->
            versionNameContinuation = cont
            versionNameOperation = VersionNameOperation.Waiting(modPackInfo)
        }
    }

    /**
     * 用户确认输入版本名称
     */
    fun confirmVersionName(name: String) {
        //恢复continuation
        versionNameContinuation?.resume(name)
        versionNameContinuation = null
        versionNameOperation = VersionNameOperation.None
    }

    /**
     * 整合包安装器
     */
    var installer by mutableStateOf<ModPackInstaller?>(null)

    fun install(
        context: Context,
        info: DownloadVersionInfo,
    ) {
        installer = ModPackInstaller(
            context = context,
            info = info,
            scope = viewModelScope,
            waitForVersionName = ::waitForVersionName
        ).also {
            it.installModPack(
                onInstalled = {
                    installer = null
                    VersionsManager.refresh()
                    installOperation = ModPackInstallOperation.Success
                },
                onError = { th ->
                    installer = null
                    installOperation = ModPackInstallOperation.Error(th)
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
private fun rememberModPackViewModel(
    key: NestedNavKey.DownloadModPack
): ModPackViewModel {
    return viewModel(
        key = key.toString()
    ) {
        ModPackViewModel()
    }
}

@Composable
fun DownloadModPackScreen(
    key: NestedNavKey.DownloadModPack,
    mainScreenKey: NavKey?,
    downloadScreenKey: NavKey?,
    downloadModPackScreenKey: NavKey?,
    onCurrentKeyChange: (NavKey?) -> Unit
) {
    val viewModel: ModPackViewModel = rememberModPackViewModel(key)

    val context = LocalContext.current
    val backStack = key.backStack
    val stackTopKey = backStack.lastOrNull()
    LaunchedEffect(stackTopKey) {
        onCurrentKeyChange(stackTopKey)
    }

    ModPackInstallOperation(
        operation = viewModel.installOperation,
        updateOperation = { viewModel.installOperation = it },
        installer = viewModel.installer,
        onInstall = { info ->
            viewModel.install(context, info)
        },
        onCancel = {
            viewModel.cancel()
        }
    )

    VersionNameOperation(
        operation = viewModel.versionNameOperation,
        confirmVersionName = { name ->
            viewModel.confirmVersionName(name)
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
                entry<NormalNavKey.SearchModPack> {
                    SearchModPackScreen(
                        mainScreenKey = mainScreenKey,
                        downloadScreenKey = downloadScreenKey,
                        downloadModPackScreenKey = key,
                        downloadModPackScreenCurrentKey = downloadModPackScreenKey
                    ) { platform, projectId, iconUrl ->
                        backStack.navigateTo(
                            NormalNavKey.DownloadAssets(platform, projectId, PlatformClasses.MOD_PACK, iconUrl)
                        )
                    }
                }
                entry<NormalNavKey.DownloadAssets> { assetsKey ->
                    DownloadAssetsScreen(
                        mainScreenKey = mainScreenKey,
                        parentScreenKey = key,
                        parentCurrentKey = downloadScreenKey,
                        currentKey = downloadModPackScreenKey,
                        key = assetsKey,
                        onItemClicked = { info ->
                            if (viewModel.installOperation !is ModPackInstallOperation.None) {
                                //不是待安装状态，拒绝此次安装
                                return@DownloadAssetsScreen
                            }
                            viewModel.installOperation = if (!NotificationManager.checkNotificationEnabled(context)) {
                                //警告通知权限
                                ModPackInstallOperation.WarningForNotification(info)
                            } else {
                                ModPackInstallOperation.Warning(info)
                            }
                        }
                    )
                }
            }
        )
    } else {
        Box(Modifier.fillMaxSize())
    }
}

@Composable
private fun ModPackInstallOperation(
    operation: ModPackInstallOperation,
    updateOperation: (ModPackInstallOperation) -> Unit,
    installer: ModPackInstaller?,
    onInstall: (DownloadVersionInfo) -> Unit,
    onCancel: () -> Unit
) {
    when (operation) {
        is ModPackInstallOperation.None -> {}
        is ModPackInstallOperation.WarningForNotification -> {
            NotificationCheck(
                onGranted = {
                    //权限被授予，开始安装
                    updateOperation(ModPackInstallOperation.Warning(operation.info))
                },
                onIgnore = {
                    //用户不想授权，但是支持继续进行安装
                    updateOperation(ModPackInstallOperation.Warning(operation.info))
                },
                onDismiss = {
                    updateOperation(ModPackInstallOperation.None)
                }
            )
        }
        is ModPackInstallOperation.Warning -> {
            //警告整合包的兼容性（免责声明）
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = {
                    Text(text = stringResource(R.string.download_modpack_warning1))
                    Text(text = stringResource(R.string.download_modpack_warning2))
                    Text(
                        text = stringResource(R.string.download_modpack_warning3),
                        fontWeight = FontWeight.Bold
                    )
                },
                confirmText = stringResource(R.string.download_install),
                onCancel = {
                    updateOperation(ModPackInstallOperation.None)
                },
                onConfirm = {
                    updateOperation(ModPackInstallOperation.Install(operation.info))
                }
            )
        }
        is ModPackInstallOperation.Install -> {
            if (installer != null) {
                val tasks = installer.tasksFlow.collectAsState()
                if (tasks.value.isNotEmpty()) {
                    //安装整合包的弹窗
                    GameInstallingDialog(
                        title = stringResource(R.string.download_modpack_install_title),
                        tasks = tasks.value,
                        onCancel = {
                            onCancel()
                            updateOperation(ModPackInstallOperation.None)
                        }
                    )
                }
            } else {
                onInstall(operation.info)
            }
        }
        is ModPackInstallOperation.Error -> {
            val th = operation.th
            lError("Failed to download the game!", th)
            val message = when (th) {
                is HttpRequestTimeoutException, is SocketTimeoutException -> stringResource(R.string.error_timeout)
                is UnknownHostException, is UnresolvedAddressException -> stringResource(R.string.error_network_unreachable)
                is ConnectException -> stringResource(R.string.error_connection_failed)
                is SerializationException, is JsonSyntaxException -> stringResource(R.string.error_parse_failed)
                is JvmCrashException -> stringResource(R.string.download_install_error_jvm_crash, th.code)
                is DownloadFailedException -> stringResource(R.string.download_install_error_download_failed)
                else -> {
                    val errorMessage = th.localizedMessage ?: th.message ?: th::class.qualifiedName ?: "Unknown error"
                    stringResource(R.string.error_unknown, errorMessage)
                }
            }
            val dismiss = {
                updateOperation(ModPackInstallOperation.None)
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
        is ModPackInstallOperation.Success -> {
            SimpleAlertDialog(
                title = stringResource(R.string.download_install_success_title),
                text = stringResource(R.string.download_install_success_message)
            ) {
                updateOperation(ModPackInstallOperation.None)
            }
        }
    }
}

@Composable
private fun VersionNameOperation(
    operation: VersionNameOperation,
    confirmVersionName: (String) -> Unit
) {
    when (operation) {
        is VersionNameOperation.None -> {}
        is VersionNameOperation.Waiting -> {
            val modpackInfo = operation.info
            var name by remember { mutableStateOf(modpackInfo.name) }
            var errorMessage by remember { mutableStateOf("") }

            val isError = name.isEmpty() || isFilenameInvalid(name) { message ->
                errorMessage = message
            } || VersionsManager.validateVersionName(name, null) { message ->
                errorMessage = message
            }

            SimpleEditDialog(
                title = stringResource(R.string.download_install_input_version_name_title),
                value = name,
                onValueChange = { name = it },
                isError = isError,
                supportingText = {
                    when {
                        name.isEmpty() -> Text(text = stringResource(R.string.generic_cannot_empty))
                        isError -> Text(text = errorMessage)
                    }
                },
                singleLine = true,
                onConfirm = {
                    if (!isError) {
                        confirmVersionName(name)
                    }
                }
            )
        }
    }
}
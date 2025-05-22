package com.movtery.zalithlauncher.ui.screens.content

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.copyLocalFile
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.addOtherServer
import com.movtery.zalithlauncher.game.account.isLocalAccount
import com.movtery.zalithlauncher.game.account.isMicrosoftAccount
import com.movtery.zalithlauncher.game.account.isMicrosoftLogging
import com.movtery.zalithlauncher.game.account.localLogin
import com.movtery.zalithlauncher.game.account.microsoft.NotPurchasedMinecraftException
import com.movtery.zalithlauncher.game.account.microsoftLogin
import com.movtery.zalithlauncher.game.account.otherserver.OtherLoginHelper
import com.movtery.zalithlauncher.game.account.otherserver.ResponseException
import com.movtery.zalithlauncher.game.account.otherserver.models.Servers
import com.movtery.zalithlauncher.game.account.saveAccount
import com.movtery.zalithlauncher.game.skin.SkinModelType
import com.movtery.zalithlauncher.game.skin.getLocalUUIDWithSkinModel
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.path.UrlManager
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.state.ObjectStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.ScalingActionButton
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.ui.components.SimpleListDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.AccountItem
import com.movtery.zalithlauncher.ui.screens.content.elements.AccountOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.AccountSkinOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.LocalLoginDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.LocalLoginOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.LoginItem
import com.movtery.zalithlauncher.ui.screens.content.elements.MicrosoftLoginOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.MicrosoftLoginTipDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.OtherLoginOperation
import com.movtery.zalithlauncher.ui.screens.content.elements.OtherServerLoginDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.SelectSkinModelDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.ServerItem
import com.movtery.zalithlauncher.ui.screens.content.elements.ServerOperation
import com.movtery.zalithlauncher.utils.CryptoManager
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.getMessageOrToString
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

const val ACCOUNT_MANAGE_SCREEN_TAG = "AccountManageScreen"

private val otherServerConfig = MutableStateFlow(Servers(ArrayList()))
private val otherServerConfigFile = File(PathManager.DIR_GAME, "other_servers.json")

private fun refreshOtherServer() {
    val text: String = otherServerConfigFile.takeIf { it.exists() }?.readText() ?: return
    val config = CryptoManager.decrypt(text)
    otherServerConfig.value = GSON.fromJson(config, Servers::class.java)
}

@Composable
fun AccountManageScreen() {
    var microsoftLoginOperation by remember { mutableStateOf<MicrosoftLoginOperation>(MicrosoftLoginOperation.None) }
    var localLoginOperation by remember { mutableStateOf<LocalLoginOperation>(LocalLoginOperation.None) }
    var otherLoginOperation by remember { mutableStateOf<OtherLoginOperation>(OtherLoginOperation.None) }
    var serverOperation by remember { mutableStateOf<ServerOperation>(ServerOperation.None) }

    BaseScreen(
        screenTag = ACCOUNT_MANAGE_SCREEN_TAG,
        currentTag = MutableStates.mainScreenTag
    ) { isVisible ->
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            ServerTypeMenu(
                isVisible = isVisible,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(all = 12.dp)
                    .weight(2.5f),
                updateMicrosoftOperation = { microsoftLoginOperation = it },
                updateLocalLoginOperation = { localLoginOperation = it },
                updateOtherLoginOperation = { otherLoginOperation = it },
                updateServerOperation = { serverOperation = it }
            )
            AccountsLayout(
                isVisible = isVisible,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 12.dp, end = 12.dp, bottom = 12.dp)
                    .weight(7.5f),
                onAddAuthClicked = {
                    //打开添加认证服务器的对话框
                    serverOperation = ServerOperation.AddNew
                }
            )
        }
    }

    //微软账号操作逻辑
    MicrosoftLoginOperation(
        microsoftLoginOperation = microsoftLoginOperation,
        updateOperation = { microsoftLoginOperation = it }
    )

    //离线账号操作逻辑
    LocalLoginOperation(
        localLoginOperation = localLoginOperation,
        updateOperation = { localLoginOperation = it }
    )

    //外置账号操作逻辑
    OtherLoginOperation(
        otherLoginOperation = otherLoginOperation,
        updateOperation = { otherLoginOperation = it }
    )

    //外置服务器操作逻辑
    ServerTypeOperation(
        serverOperation = serverOperation,
        updateServerOperation = { serverOperation = it }
    )
}

@Composable
private fun ServerTypeMenu(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    updateMicrosoftOperation: (MicrosoftLoginOperation) -> Unit,
    updateLocalLoginOperation: (LocalLoginOperation) -> Unit,
    updateOtherLoginOperation: (OtherLoginOperation) -> Unit,
    updateServerOperation: (ServerOperation) -> Unit
) {
    val xOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible
    )

    LaunchedEffect(true) {
        runCatching {
            refreshOtherServer()
        }.onFailure {
            Log.w("ServerTypeTab", "Failed to refresh other server", it)
        }
    }

    Card(
        modifier = modifier
            .offset {
                IntOffset(
                    x = xOffset.roundToPx(),
                    y = 0
                )
            }
            .fillMaxHeight(),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column {
            Column(
                modifier = Modifier
                    .padding(all = 12.dp)
                    .verticalScroll(state = rememberScrollState())
                    .weight(1f)
            ) {
                LoginItem(
                    modifier = Modifier.fillMaxWidth(),
                    serverName = stringResource(R.string.account_type_microsoft),
                ) {
                    if (!isMicrosoftLogging()) {
                        updateMicrosoftOperation(MicrosoftLoginOperation.Tip)
                    }
                }
                LoginItem(
                    modifier = Modifier.fillMaxWidth(),
                    serverName = stringResource(R.string.account_type_local)
                ) {
                    updateLocalLoginOperation(LocalLoginOperation.Edit)
                }

                val servers by otherServerConfig.collectAsState()
                servers.server.forEachIndexed { index, server ->
                    ServerItem(
                        server = server,
                        onClick = { updateOtherLoginOperation(OtherLoginOperation.OnLogin(server)) },
                        onDeleteClick = { updateServerOperation(ServerOperation.Delete(server.serverName, index)) }
                    )
                }
            }

            ScalingActionButton(
                modifier = Modifier
                    .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))
                    .fillMaxWidth(),
                onClick = { updateServerOperation(ServerOperation.AddNew) }
            ) {
                Text(
                    text = stringResource(R.string.account_add_new_server_button)
                )
            }
        }
    }
}

/**
 * 微软账号登陆操作逻辑
 */
@Composable
private fun MicrosoftLoginOperation(
    microsoftLoginOperation: MicrosoftLoginOperation,
    updateOperation: (MicrosoftLoginOperation) -> Unit = {}
) {
    val context = LocalContext.current

    when (microsoftLoginOperation) {
        is MicrosoftLoginOperation.None -> {}
        is MicrosoftLoginOperation.Tip -> {
            MicrosoftLoginTipDialog(
                onDismissRequest = { updateOperation(MicrosoftLoginOperation.None) },
                onConfirm = { updateOperation(MicrosoftLoginOperation.RunTask) }
            )
        }
        is MicrosoftLoginOperation.RunTask -> {
            microsoftLogin(
                context = context,
                updateOperation = { updateOperation(it) }
            )
            updateOperation(MicrosoftLoginOperation.None)
        }
    }
}

/**
 * 离线账号登陆操作逻辑
 */
@Composable
private fun LocalLoginOperation(
    localLoginOperation: LocalLoginOperation,
    updateOperation: (LocalLoginOperation) -> Unit = {}
) {
    when (localLoginOperation) {
        is LocalLoginOperation.None -> {}
        is LocalLoginOperation.Edit -> {
            LocalLoginDialog(
                onDismissRequest = { updateOperation(LocalLoginOperation.None) },
                onConfirm = { isUserNameInvalid, userName ->
                    val operation = if (isUserNameInvalid) {
                        LocalLoginOperation.Alert(userName)
                    } else {
                        LocalLoginOperation.Create(userName)
                    }
                    updateOperation(operation)
                }
            )
        }
        is LocalLoginOperation.Create -> {
            localLogin(userName = localLoginOperation.userName)
            //复位
            updateOperation(LocalLoginOperation.None)
        }
        is LocalLoginOperation.Alert -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_supporting_username_invalid_title),
                text = {
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint1))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_supporting_username_invalid_local_message_hint2),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint3))
                    Text(text = stringResource(R.string.account_supporting_username_invalid_local_message_hint4))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_supporting_username_invalid_local_message_hint5),
                        fontWeight = FontWeight.Bold
                    )
                },
                confirmText = stringResource(R.string.account_supporting_username_invalid_still_use),
                onConfirm = {
                    updateOperation(LocalLoginOperation.Create(localLoginOperation.userName))
                },
                onCancel = {
                    updateOperation(LocalLoginOperation.None)
                }
            )
        }
    }
}

@Composable
private fun OtherLoginOperation(
    otherLoginOperation: OtherLoginOperation,
    updateOperation: (OtherLoginOperation) -> Unit = {}
) {
    val context = LocalContext.current
    when (otherLoginOperation) {
        is OtherLoginOperation.None -> {}
        is OtherLoginOperation.OnLogin -> {
            OtherServerLoginDialog(
                server = otherLoginOperation.server,
                onRegisterClick = { url ->
                    NetWorkUtils.openLink(context, url)
                    updateOperation(OtherLoginOperation.None)
                },
                onDismissRequest = { updateOperation(OtherLoginOperation.None) },
                onConfirm = { email, password ->
                    updateOperation(OtherLoginOperation.None)
                    OtherLoginHelper(
                        otherLoginOperation.server, email, password,
                        onSuccess = { account, task ->
                            task.updateMessage(R.string.account_logging_in_saving)
                            account.downloadSkin()
                            saveAccount(account)
                        },
                        onFailed = { th ->
                            updateOperation(OtherLoginOperation.OnFailed(th))
                        }
                    ).createNewAccount(context) { availableProfiles, selectedFunction ->
                        updateOperation(
                            OtherLoginOperation.SelectRole(
                                availableProfiles,
                                selectedFunction
                            )
                        )
                    }
                }
            )
        }
        is OtherLoginOperation.OnFailed -> {
            val message: String = when (val th = otherLoginOperation.th) {
                is ResponseException -> th.responseMessage
                is HttpRequestTimeoutException -> stringResource(R.string.error_timeout)
                is UnknownHostException, is UnresolvedAddressException -> stringResource(R.string.error_network_unreachable)
                is ConnectException -> stringResource(R.string.error_connection_failed)
                is io.ktor.client.plugins.ResponseException -> {
                    val statusCode = th.response.status
                    val res = when (statusCode) {
                        HttpStatusCode.Unauthorized -> R.string.error_unauthorized
                        HttpStatusCode.NotFound -> R.string.error_notfound
                        else -> R.string.error_client_error
                    }
                    stringResource(res, statusCode)
                }
                else -> {
                    Log.e("OtherLoginOperation", "An unknown exception was caught!", th)
                    val errorMessage = th.localizedMessage ?: th.message ?: th::class.qualifiedName ?: "Unknown error"
                    stringResource(R.string.error_unknown, errorMessage)
                }
            }

            ObjectStates.updateThrowable(
                ObjectStates.ThrowableMessage(
                    title = stringResource(R.string.account_logging_in_failed),
                    message = message
                )
            )
            updateOperation(OtherLoginOperation.None)
        }
        is OtherLoginOperation.SelectRole -> {
            SimpleListDialog(
                title = stringResource(R.string.account_other_login_select_role),
                itemsProvider = { otherLoginOperation.profiles },
                itemTextProvider = { it.name },
                onItemSelected = { otherLoginOperation.selected(it) },
                onDismissRequest = { updateOperation(OtherLoginOperation.None) }
            )
        }
    }
}

@Composable
private fun ServerTypeOperation(
    serverOperation: ServerOperation,
    updateServerOperation: (ServerOperation) -> Unit
) {
    when (serverOperation) {
        is ServerOperation.AddNew -> {
            var serverUrl by rememberSaveable { mutableStateOf("") }
            SimpleEditDialog(
                title = stringResource(R.string.account_add_new_server),
                value = serverUrl,
                onValueChange = { serverUrl = it.trim() },
                label = { Text(text = stringResource(R.string.account_label_server_url)) },
                singleLine = true,
                onDismissRequest = { updateServerOperation(ServerOperation.None) },
                onConfirm = {
                    if (serverUrl.isNotEmpty()) {
                        updateServerOperation(ServerOperation.Add(serverUrl))
                    }
                }
            )
        }
        is ServerOperation.Add -> {
            addOtherServer(
                serverUrl = serverOperation.serverUrl,
                serverConfig = { otherServerConfig },
                serverConfigFile = otherServerConfigFile,
                onThrowable = { updateServerOperation(ServerOperation.OnThrowable(it)) }
            )
            updateServerOperation(ServerOperation.None)
        }
        is ServerOperation.Delete -> {
            SimpleAlertDialog(
                title = stringResource(R.string.account_other_login_delete_server_title),
                text = stringResource(
                    R.string.account_other_login_delete_server_message,
                    serverOperation.serverName
                ),
                onDismiss = { updateServerOperation(ServerOperation.None) },
                onConfirm = {
                    otherServerConfig.update { currentConfig ->
                        currentConfig.server.removeAt(serverOperation.serverIndex)
                        val configString = GSON.toJson(currentConfig, Servers::class.java)
                        val text = CryptoManager.encrypt(configString)
                        runCatching {
                            otherServerConfigFile.writeText(text)
                        }.onFailure {
                            Log.e("ServerTypeTab", "Failed to save other server config", it)
                        }
                        currentConfig.copy()
                    }
                    updateServerOperation(ServerOperation.None)
                }
            )
        }
        is ServerOperation.OnThrowable -> {
            ObjectStates.updateThrowable(
                ObjectStates.ThrowableMessage(
                    title = stringResource(R.string.account_other_login_adding_failure),
                    message = serverOperation.throwable.getMessageOrToString()
                )
            )
            updateServerOperation(ServerOperation.None)
        }
        is ServerOperation.None -> {}
    }
}

@Composable
private fun AccountsLayout(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    onAddAuthClicked: () -> Unit = {}
) {
    val yOffset by swapAnimateDpAsState(
        targetValue = (-40).dp,
        swapIn = isVisible
    )

    val accounts by AccountsManager.accountsFlow.collectAsState()
    val currentAccount by AccountsManager.currentAccountFlow.collectAsState()

    Card(
        modifier = modifier.offset {
            IntOffset(
                x = 0,
                y = yOffset.roundToPx()
            )
        },
        shape = MaterialTheme.shapes.extraLarge
    ) {
        var accountOperation by remember { mutableStateOf<AccountOperation>(AccountOperation.None) }
        AccountOperation(
            accountOperation = accountOperation,
            updateAccountOperation = { accountOperation = it }
        )

        if (accounts.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape = MaterialTheme.shapes.extraLarge),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                items(accounts) { account ->
                    var accountSkinOperation by remember { mutableStateOf<AccountSkinOperation>(AccountSkinOperation.None) }
                    AccountSkinOperation(
                        account = account,
                        accountSkinOperation = accountSkinOperation,
                        updateOperation = { accountSkinOperation = it },
                        onAddAuthClicked = onAddAuthClicked
                    )

                    val skinPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        uri?.let { result ->
                            accountSkinOperation = AccountSkinOperation.SelectSkinModel(result)
                        }
                    }

                    val context = LocalContext.current
                    AccountItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        currentAccount = currentAccount,
                        account = account,
                        onSelected = { uniqueUUID ->
                            AccountsManager.setCurrentAccount(uniqueUUID)
                        },
                        onChangeSkin = {
                            if (account.isMicrosoftAccount()) {
                                NetWorkUtils.openLink(context = context, link = UrlManager.URL_MINECRAFT_CHANGE_SKIN)
                            } else if (account.isLocalAccount()) {
                                skinPicker.launch(arrayOf("image/*"))
                            }
                        },
                        onResetSkin = {
                            accountSkinOperation = AccountSkinOperation.PreResetSkin
                        },
                        onRefreshClick = { accountOperation = AccountOperation.Refresh(account) },
                        onDeleteClick = { accountOperation = AccountOperation.Delete(account) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                ScalingLabel(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(R.string.account_no_account)
                )
            }
        }
    }
}

@Composable
private fun AccountSkinOperation(
    account: Account,
    accountSkinOperation: AccountSkinOperation,
    updateOperation: (AccountSkinOperation) -> Unit,
    onAddAuthClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    when (accountSkinOperation) {
        is AccountSkinOperation.None -> {}
        is AccountSkinOperation.SaveSkin -> {
            val skinFile = account.getSkinFile()
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = {
                        context.copyLocalFile(accountSkinOperation.uri, skinFile)
                        saveAccount(account)
                        //警告用户关于自定义皮肤的一些注意事项
                        updateOperation(AccountSkinOperation.AlertModel)
                    },
                    onError = { th ->
                        FileUtils.deleteQuietly(skinFile)
                        ObjectStates.updateThrowable(
                            ObjectStates.ThrowableMessage(
                                title = context.getString(R.string.error_import_image),
                                message = th.getMessageOrToString()
                            )
                        )
                        updateOperation(AccountSkinOperation.None)
                    }
                )
            )
        }
        is AccountSkinOperation.SelectSkinModel -> {
            SelectSkinModelDialog(
                onDismissRequest = {
                    updateOperation(AccountSkinOperation.None)
                },
                onSelected = { type ->
                    account.skinModelType = type
                    account.profileId = getLocalUUIDWithSkinModel(account.username, type)
                    updateOperation(AccountSkinOperation.SaveSkin(accountSkinOperation.uri))
                }
            )
        }
        is AccountSkinOperation.AlertModel -> {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        text = stringResource(R.string.generic_warning),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(text = stringResource(R.string.account_change_skin_select_model_alert_hint1))
                        Text(
                            text = stringResource(R.string.account_change_skin_select_model_alert_hint2),
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = stringResource(R.string.account_change_skin_select_model_alert_hint3))
                        Text(text = stringResource(R.string.account_change_skin_select_model_alert_hint4))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.account_change_skin_select_model_alert_hint5),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        IconTextButton(
                            onClick = {
                                onAddAuthClicked()
                                updateOperation(AccountSkinOperation.None)
                            },
                            imageVector = Icons.Outlined.Dns,
                            contentDescription = null,
                            text = stringResource(R.string.account_change_skin_select_model_alert_auth_server)
                        )
                        IconTextButton(
                            onClick = {
                                NetWorkUtils.openLink(context, context.getString(R.string.url_mod_custom_skin_loader))
                                updateOperation(AccountSkinOperation.None)
                            },
                            imageVector = Icons.Outlined.Checkroom,
                            contentDescription = null,
                            text = stringResource(R.string.account_change_skin_select_model_alert_custom_skin_loader)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            updateOperation(AccountSkinOperation.None)
                        }
                    ) {
                        Text(text = stringResource(R.string.generic_go_it))
                    }
                }
            )
        }
        is AccountSkinOperation.PreResetSkin -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_reset),
                text = stringResource(R.string.account_change_skin_reset_skin_message),
                onDismiss = { updateOperation(AccountSkinOperation.None) },
                onConfirm = { updateOperation(AccountSkinOperation.ResetSkin) }
            )
        }
        is AccountSkinOperation.ResetSkin -> {
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = {
                        account.apply {
                            FileUtils.deleteQuietly(getSkinFile())
                            skinModelType = SkinModelType.NONE
                            profileId = getLocalUUIDWithSkinModel(username, skinModelType)
                            saveAccount(this)
                        }
                    }
                )
            )
            updateOperation(AccountSkinOperation.None)
        }
    }
}

@Composable
private fun AccountOperation(
    accountOperation: AccountOperation,
    updateAccountOperation: (AccountOperation) -> Unit
) {
    val context = LocalContext.current
    when (accountOperation) {
        is AccountOperation.Delete -> {
            //删除账号前弹出Dialog提醒
            SimpleAlertDialog(
                title = stringResource(R.string.account_delete_title),
                text = stringResource(R.string.account_delete_message,
                    accountOperation.account.username),
                onConfirm = {
                    AccountsManager.deleteAccount(accountOperation.account)
                    updateAccountOperation(AccountOperation.None)
                },
                onDismiss = { updateAccountOperation(AccountOperation.None) }
            )
        }
        is AccountOperation.Refresh -> {
            if (NetWorkUtils.isNetworkAvailable(context)) {
                AccountsManager.performLogin(
                    context = context,
                    account = accountOperation.account,
                    onSuccess = { account, task ->
                        task.updateMessage(R.string.account_logging_in_saving)
                        account.downloadSkin()
                        saveAccount(account)
                    },
                    onFailed = { updateAccountOperation(AccountOperation.OnFailed(it)) }
                )
            }
            updateAccountOperation(AccountOperation.None)
        }
        is AccountOperation.OnFailed -> {
            val message: String = when (val th = accountOperation.th) {
                is NotPurchasedMinecraftException -> stringResource(R.string.account_logging_not_purchased_minecraft)
                is ResponseException -> th.responseMessage
                is HttpRequestTimeoutException -> stringResource(R.string.error_timeout)
                is UnknownHostException, is UnresolvedAddressException -> stringResource(R.string.error_network_unreachable)
                is ConnectException -> stringResource(R.string.error_connection_failed)
                is io.ktor.client.plugins.ResponseException -> {
                    val statusCode = th.response.status
                    val res = when (statusCode) {
                        HttpStatusCode.Unauthorized -> R.string.error_unauthorized
                        HttpStatusCode.NotFound -> R.string.error_notfound
                        else -> R.string.error_client_error
                    }
                    stringResource(res, statusCode)
                }
                else -> {
                    Log.e("AccountOperation", "An unknown exception was caught!", th)
                    val errorMessage = th.localizedMessage ?: th.message ?: th::class.qualifiedName ?: "Unknown error"
                    stringResource(R.string.error_unknown, errorMessage)
                }
            }
            ObjectStates.updateThrowable(
                ObjectStates.ThrowableMessage(
                    title = stringResource(R.string.account_logging_in_failed),
                    message = message
                )
            )
            updateAccountOperation(AccountOperation.None)
        }
        is AccountOperation.None -> {}
    }
}

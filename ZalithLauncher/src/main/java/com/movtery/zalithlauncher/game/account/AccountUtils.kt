package com.movtery.zalithlauncher.game.account

import android.content.Context
import android.widget.Toast
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.account.auth_server.AuthServerApi
import com.movtery.zalithlauncher.game.account.auth_server.AuthServerHelper
import com.movtery.zalithlauncher.game.account.auth_server.data.AuthServer
import com.movtery.zalithlauncher.game.account.microsoft.AsyncStatus
import com.movtery.zalithlauncher.game.account.microsoft.AuthType
import com.movtery.zalithlauncher.game.account.microsoft.MicrosoftAuthenticator
import com.movtery.zalithlauncher.game.account.microsoft.MinecraftProfileException
import com.movtery.zalithlauncher.game.account.microsoft.NotPurchasedMinecraftException
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException
import com.movtery.zalithlauncher.game.account.microsoft.toLocal
import com.movtery.zalithlauncher.state.ObjectStates
import com.movtery.zalithlauncher.ui.screens.content.WebViewScreenKey
import com.movtery.zalithlauncher.ui.screens.content.elements.MicrosoftLoginOperation
import com.movtery.zalithlauncher.ui.screens.content.navigateToWeb
import com.movtery.zalithlauncher.ui.screens.main.elements.backToMainScreen
import com.movtery.zalithlauncher.ui.screens.main.elements.mainScreenBackStack
import com.movtery.zalithlauncher.ui.screens.main.elements.mainScreenKey
import com.movtery.zalithlauncher.utils.copyText
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException
import java.util.Locale
import java.util.Objects
import kotlin.coroutines.CoroutineContext

fun Account.isAuthServerAccount(): Boolean {
    return !Objects.isNull(otherBaseUrl) && otherBaseUrl != "0"
}

fun Account.isMicrosoftAccount(): Boolean {
    return accountType == AccountType.MICROSOFT.tag
}

fun Account.isLocalAccount(): Boolean {
    return accountType == AccountType.LOCAL.tag
}

fun Account?.isNoLoginRequired(): Boolean {
    return this == null || isLocalAccount()
}

fun Account.isSkinChangeAllowed(): Boolean {
    return isMicrosoftAccount() || isLocalAccount()
}

fun Account.accountTypePriority(): Int {
    return when (this.accountType) {
        AccountType.MICROSOFT.tag -> 0 //微软账号优先
        null -> Int.MAX_VALUE
        else -> 1
    }
}

private const val MICROSOFT_LOGGING_TASK = "microsoft_logging_task"

/**
 * 检查当前微软账号登陆是否正在进行中
 */
fun isMicrosoftLogging() = TaskSystem.containsTask(MICROSOFT_LOGGING_TASK)

fun microsoftLogin(
    context: Context,
    updateOperation: (MicrosoftLoginOperation) -> Unit
) {
    val task = Task.runTask(
        id = MICROSOFT_LOGGING_TASK,
        dispatcher = Dispatchers.IO,
        task = { task ->
            task.updateProgress(-1f, R.string.account_microsoft_fetch_device_code)
            val deviceCode = MicrosoftAuthenticator.fetchDeviceCodeResponse(coroutineContext)
            copyText(null, deviceCode.userCode, context)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.account_microsoft_coped_device_code, deviceCode.userCode),
                    Toast.LENGTH_SHORT
                ).show()
            }
            mainScreenBackStack.navigateToWeb(deviceCode.verificationUrl)
            task.updateProgress(-1f, R.string.account_microsoft_get_token, deviceCode.userCode)
            val tokenResponse = MicrosoftAuthenticator.getTokenResponse(deviceCode, coroutineContext) {
                !WebViewScreenKey::class.java.isInstance(mainScreenKey)
            }
            mainScreenBackStack.backToMainScreen()
            val account = authAsync(
                AuthType.Access,
                tokenResponse.refreshToken,
                tokenResponse.accessToken,
                coroutineContext = coroutineContext,
                updateProgress = task::updateProgress
            )
            task.updateMessage(R.string.account_logging_in_saving)
            account.downloadSkin()
            AccountsManager.saveAccount(account)
        },
        onError = { th ->
            when (th) {
                is HttpRequestTimeoutException -> context.getString(R.string.account_logging_time_out)
                is NotPurchasedMinecraftException -> toLocal(context)
                is MinecraftProfileException -> th.toLocal(context)
                is XboxLoginException -> th.toLocal(context)
                is UnknownHostException, is UnresolvedAddressException -> context.getString(R.string.error_network_unreachable)
                is ConnectException -> context.getString(R.string.error_connection_failed)
                is ResponseException -> {
                    val statusCode = th.response.status
                    val res = when (statusCode) {
                        HttpStatusCode.Unauthorized -> R.string.error_unauthorized
                        HttpStatusCode.NotFound -> R.string.error_notfound
                        else -> R.string.error_client_error
                    }
                    context.getString(res, statusCode)
                }
                is CancellationException -> { null }
                else -> {
                    val errorMessage = th.localizedMessage ?: th.message ?: th::class.qualifiedName ?: "Unknown error"
                    context.getString(R.string.error_unknown, errorMessage)
                }
            }?.let { message ->
                ObjectStates.updateThrowable(
                    ObjectStates.ThrowableMessage(
                        title = context.getString(R.string.account_logging_in_failed),
                        message = message
                    )
                )
            }
        },
        onFinally = {
            updateOperation(MicrosoftLoginOperation.None)
        }
    )

    TaskSystem.submitTask(task)
}

private suspend fun authAsync(
    authType: AuthType,
    refreshToken: String,
    accessToken: String = "NULL",
    coroutineContext: CoroutineContext,
    updateProgress: (Float, Int) -> Unit
): Account {
    return MicrosoftAuthenticator.authAsync(authType, refreshToken, accessToken, coroutineContext) { asyncStatus ->
        when (asyncStatus) {
            AsyncStatus.GETTING_ACCESS_TOKEN ->     updateProgress(0.25f, R.string.account_microsoft_getting_access_token)
            AsyncStatus.GETTING_XBL_TOKEN ->        updateProgress(0.4f, R.string.account_microsoft_getting_xbl_token)
            AsyncStatus.GETTING_XSTS_TOKEN ->       updateProgress(0.55f, R.string.account_microsoft_getting_xsts_token)
            AsyncStatus.AUTHENTICATE_MINECRAFT ->   updateProgress(0.7f, R.string.account_microsoft_authenticate_minecraft)
            AsyncStatus.VERIFY_GAME_OWNERSHIP ->    updateProgress(0.85f, R.string.account_microsoft_verify_game_ownership)
            AsyncStatus.GETTING_PLAYER_PROFILE ->   updateProgress(1f, R.string.account_microsoft_getting_player_profile)
        }
    }
}

fun microsoftRefresh(
    account: Account,
    onSuccess: suspend (Account, Task) -> Unit,
    onFailed: (th: Throwable) -> Unit = {},
    onFinally: () -> Unit = {}
): Task? {
    if (TaskSystem.containsTask(account.profileId)) return null

    return Task.runTask(
        id = account.profileId,
        dispatcher = Dispatchers.IO,
        task = { task ->
            val newAcc = authAsync(
                AuthType.Refresh,
                account.refreshToken,
                account.accessToken,
                coroutineContext = coroutineContext,
                updateProgress = task::updateProgress
            )
            account.apply {
                this.accessToken = newAcc.accessToken
                this.clientToken = newAcc.clientToken
                this.profileId = newAcc.profileId
                this.username = newAcc.username
                this.refreshToken = newAcc.refreshToken
                this.xUid = newAcc.xUid
            }
            onSuccess(account, task)
        },
        onError = { e ->
            if (e is CancellationException) return@runTask
            onFailed(e)
        },
        onFinally = onFinally
    )
}

fun otherLogin(
    context: Context,
    account: Account,
    onSuccess: suspend (Account, task: Task) -> Unit = { _, _ -> },
    onFailed: (th: Throwable) -> Unit = {},
    onFinally: () -> Unit = {}
): Task? {
    if (TaskSystem.containsTask(account.uniqueUUID)) return null

    return AuthServerHelper(
        baseUrl = account.otherBaseUrl!!,
        serverName = account.accountType!!,
        email = account.otherAccount!!,
        password = account.otherPassword!!,
        onSuccess = onSuccess,
        onFailed = onFailed,
        onFinally = onFinally
    ).justLogin(context, account)
}

/**
 * 离线账号登陆
 */
fun localLogin(userName: String) {
    val account = Account(
        username = userName,
        accountType = AccountType.LOCAL.tag
    )
    AccountsManager.saveAccount(account)
}

fun addOtherServer(
    serverUrl: String,
    onThrowable: (Throwable) -> Unit = {}
) {
    val task = Task.runTask(
        task = { task ->
            task.updateProgress(-1f, R.string.account_other_login_getting_full_url)
            val fullServerUrl = tryGetFullServerUrl(serverUrl)
            ensureActive()
            task.updateProgress(0.5f, R.string.account_other_login_getting_server_info)
            AuthServerApi.getServeInfo(fullServerUrl)?.let { data ->
                JSONObject(data).optJSONObject("meta")?.let { meta ->
                    if (AccountsManager.isAuthServerExists(fullServerUrl)) {
                        //确保服务器不重复
                        return@runTask
                    }
                    val server = AuthServer(
                        serverName = meta.optString("serverName"),
                        baseUrl = fullServerUrl,
                        register = meta.optJSONObject("links")?.optString("register") ?: ""
                    )
                    task.updateProgress(0.8f, R.string.account_other_login_saving_server)
                    AccountsManager.saveAuthServer(server)
                    task.updateProgress(1f, R.string.generic_done)
                }
            }
        },
        onError = { e ->
            onThrowable(e)
            lError("Failed to add auth server", e)
        }
    )

    TaskSystem.submitTask(task)
}

/**
 * 获取账号类型名称
 */
fun getAccountTypeName(context: Context, account: Account): String {
    return if (account.isMicrosoftAccount()) {
        context.getString(R.string.account_type_microsoft)
    } else if (account.isAuthServerAccount()) {
        account.accountType ?: "Unknown"
    } else {
        context.getString(R.string.account_type_local)
    }
}

/**
 * 修改自源代码：[HMCL Core: AuthlibInjectorServer.java](https://github.com/HMCL-dev/HMCL/blob/main/HMCLCore/src/main/java/org/jackhuang/hmcl/auth/authlibinjector/AuthlibInjectorServer.java#L60-#L76)
 * <br>原项目版权归原作者所有，遵循GPL v3协议
 */
fun tryGetFullServerUrl(baseUrl: String): String {
    fun String.addSlashIfMissing(): String {
        if (!endsWith("/")) return "$this/"
        return this
    }

    var url = addHttpsIfMissing(baseUrl)
    runCatching {
        var conn = URL(url).openConnection() as HttpURLConnection
        conn.getHeaderField("x-authlib-injector-api-location")?.let { ali ->
            val absoluteAli = URL(conn.url, ali)
            url = url.addSlashIfMissing()
            val absoluteUrl = absoluteAli.toString().addSlashIfMissing()
            if (url != absoluteUrl) {
                conn.disconnect()
                url = absoluteUrl
                conn = absoluteAli.openConnection() as HttpURLConnection
            }
        }

        return url.addSlashIfMissing()
    }.getOrElse { e ->
        lError("Failed to get full server url", e)
    }
    return baseUrl
}

/**
 * 修改自源代码：[HMCL Core: AuthlibInjectorServer.java](https://github.com/HMCL-dev/HMCL/blob/main/HMCLCore/src/main/java/org/jackhuang/hmcl/auth/authlibinjector/AuthlibInjectorServer.java#L90-#L96)
 * <br>原项目版权归原作者所有，遵循GPL v3协议
 */
private fun addHttpsIfMissing(baseUrl: String): String {
    return if (!baseUrl.startsWith("http://", true) && !baseUrl.startsWith("https://")) {
        "https://$baseUrl".lowercase(Locale.ROOT)
    } else baseUrl.lowercase(Locale.ROOT)
}

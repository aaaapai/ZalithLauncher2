package com.movtery.zalithlauncher.game.launch

import android.content.Context
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.auth_server.ResponseException
import com.movtery.zalithlauncher.game.account.microsoft.MinecraftProfileException
import com.movtery.zalithlauncher.game.account.microsoft.NotPurchasedMinecraftException
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException
import com.movtery.zalithlauncher.game.account.microsoft.toLocal
import com.movtery.zalithlauncher.game.version.download.DownloadMode
import com.movtery.zalithlauncher.game.version.download.MinecraftDownloader
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.state.ObjectStates
import com.movtery.zalithlauncher.ui.activities.runGame
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import java.net.ConnectException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

object LaunchGame {
    private var isLaunching: Boolean = false

    fun launchGame(
        context: Context,
        version: Version
    ) {
        if (isLaunching) return

        val account = AccountsManager.getCurrentAccount() ?: return

        isLaunching = true

        val downloadTask = MinecraftDownloader(
            context = context,
            version = version.getVersionInfo()?.minecraftVersion ?: version.getVersionName(),
            customName = version.getVersionName(),
            verifyIntegrity = !version.skipGameIntegrityCheck(),
            mode = DownloadMode.VERIFY_AND_REPAIR,
            onCompletion = {
                runGame(context, version)
            },
            onError = { message ->
                ObjectStates.updateThrowable(
                    ObjectStates.ThrowableMessage(
                        title = context.getString(R.string.minecraft_download_failed),
                        message = message
                    )
                )
            }
        ).getDownloadTask()

        fun runDownloadTask() {
            TaskSystem.submitTask(downloadTask) { isLaunching = false }
        }

        val loginTask = if (NetWorkUtils.isNetworkAvailable(context)) {
            AccountsManager.performLoginTask(
                context = context,
                account = account,
                onSuccess = { acc, _ ->
                    AccountsManager.suspendSaveAccount(acc)
                },
                onFailed = { error ->
                    val message: String = when (error) {
                        is NotPurchasedMinecraftException -> toLocal(context)
                        is MinecraftProfileException -> error.toLocal(context)
                        is XboxLoginException -> error.toLocal(context)
                        is ResponseException -> error.responseMessage
                        is HttpRequestTimeoutException -> context.getString(R.string.error_timeout)
                        is UnknownHostException, is UnresolvedAddressException -> context.getString(R.string.error_network_unreachable)
                        is ConnectException -> context.getString(R.string.error_connection_failed)
                        is io.ktor.client.plugins.ResponseException -> {
                            val statusCode = error.response.status
                            val res = when (statusCode) {
                                HttpStatusCode.Unauthorized -> R.string.error_unauthorized
                                HttpStatusCode.NotFound -> R.string.error_notfound
                                else -> R.string.error_client_error
                            }
                            context.getString(res, statusCode)
                        }
                        else -> {
                            lError("An unknown exception was caught!", error)
                            val errorMessage = error.localizedMessage ?: error.message ?: error::class.qualifiedName ?: "Unknown error"
                            context.getString(R.string.error_unknown, errorMessage)
                        }
                    }

                    ObjectStates.updateThrowable(
                        ObjectStates.ThrowableMessage(
                            title = context.getString(R.string.account_logging_in_failed),
                            message = message
                        )
                    )
                },
                onFinally = { runDownloadTask() }
            )
        } else {
            null
        }

        loginTask?.let { task ->
            TaskSystem.submitTask(task)
        } ?: run {
            version.offlineAccountLogin = true
            runDownloadTask()
        }
    }
}

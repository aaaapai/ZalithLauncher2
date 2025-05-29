package com.movtery.zalithlauncher.game.account.auth_server

import android.content.Context
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.auth_server.data.AuthServer
import com.movtery.zalithlauncher.game.account.auth_server.models.AuthResult
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import kotlinx.coroutines.Dispatchers
import java.util.Objects

/**
 * 帮助登录外置账号（创建新的外置账号、仅登录当前外置账号）
 */
class AuthServerHelper(
    private val baseUrl: String,
    private val serverName: String,
    private val email: String,
    private val password: String,
    private val onSuccess: suspend (Account, Task) -> Unit = { _, _ -> },
    private val onFailed: (th: Throwable) -> Unit = {},
    private val onFinally: () -> Unit = {}
) {
    constructor(
        server: AuthServer,
        email: String,
        password: String,
        onSuccess: suspend (Account, Task) -> Unit = { _, _ -> },
        onFailed: (th: Throwable) -> Unit = {},
        onFinally: () -> Unit = {}
    ): this(server.baseUrl, server.serverName, email, password, onSuccess, onFailed, onFinally)

    private fun login(
        context: Context,
        taskId: String? = null,
        loggingString: String = serverName,
        onlyOneRole: suspend (AuthResult, Task) -> Unit = { _, _ -> },
        hasMultipleRoles: suspend (AuthResult, Task) -> Unit = { _, _ -> }
    ) : Task {
        return Task.runTask(
            id = taskId,
            dispatcher = Dispatchers.IO,
            task = { task ->
                AuthServerApi.setBaseUrl(baseUrl)
                AuthServerApi.login(
                    context, email, password,
                    onSuccess = { authResult ->
                        if (!Objects.isNull(authResult.selectedProfile)) {
                            onlyOneRole(authResult, task)
                        } else if (!Objects.isNull(authResult.availableProfiles)) {
                            hasMultipleRoles(authResult, task)
                        }
                    },
                    onFailed = onFailed
                )
            },
            onError = { e ->
                lError("An exception was encountered while performing the login task.", e)
                onFailed(e)
            },
            onFinally = onFinally
        ).apply { updateMessage(R.string.account_logging_in, loggingString) }
    }

    private fun updateAccountInfo(
        account: Account,
        authResult: AuthResult,
        userName: String,
        profileId: String
    ) {
        account.apply {
            this.accessToken = authResult.accessToken
            this.clientToken = authResult.clientToken
            this.otherBaseUrl = baseUrl
            this.otherAccount = email
            this.otherPassword = password
            this.accountType = serverName
            this.username = userName
            this.profileId = profileId
        }
    }

    /**
     * 通过账号密码，登录一个新的账号
     * @param selectRole 当账号拥有多个角色时，需要选择角色
     */
    fun createNewAccount(
        context: Context,
        selectRole: (List<AuthResult.AvailableProfiles>, (AuthResult.AvailableProfiles) -> Unit) -> Unit
    ) {
        val task = login(
            context,
            onlyOneRole = { authResult, task ->
                val profileId = authResult.selectedProfile!!.id
                val account: Account = AccountsManager.loadFromProfileID(profileId) ?: Account()
                updateAccountInfo(account, authResult, authResult.selectedProfile!!.name, profileId)
                onSuccess(account, task)
            },
            hasMultipleRoles = { authResult, _ ->
                selectRole(authResult.availableProfiles!!) { selectedProfile ->
                    val profileId = selectedProfile.id
                    val account: Account = AccountsManager.loadFromProfileID(profileId) ?: Account()
                    updateAccountInfo(account, authResult, selectedProfile.name, profileId)
                    refresh(context, account)
                }
            }
        )
        TaskSystem.submitTask(task)
    }

    /**
     * 仅仅只是登录外置账号（使用账号密码登录）
     * JUST DO IT!!!
     * @return 登陆的任务对象
     */
    fun justLogin(context: Context, account: Account): Task {
        fun roleNotFound() { //未找到匹配的ID
            onFailed(ResponseException(context.getString(R.string.account_other_login_role_not_found)))
        }

        return login(
            context,
            onlyOneRole = { authResult, task ->
                if (authResult.selectedProfile!!.id != account.profileId) {
                    roleNotFound()
                    return@login
                }
                updateAccountInfo(account, authResult, authResult.selectedProfile!!.name, authResult.selectedProfile!!.id)
                onSuccess(account, task)
            },
            hasMultipleRoles = { authResult, task ->
                authResult.availableProfiles!!.forEach { profile ->
                    if (profile.id == account.profileId) {
                        //匹配当前账号的ID时，那么这个角色就是这个账号
                        updateAccountInfo(account, authResult, profile.name, profile.id)
                        onSuccess(account, task)
                        return@login
                    }
                }
                roleNotFound()
            },
            taskId = account.uniqueUUID,
            loggingString = account.username
        )
    }

    private fun refresh(context: Context, account: Account) {
        val task = Task.runTask(
            task = { task ->
                AuthServerApi.setBaseUrl(baseUrl)
                AuthServerApi.refresh(context, account, true,
                    onSuccess = { authResult ->
                        account.accessToken = authResult.accessToken
                        onSuccess(account, task)
                    },
                    onFailed = onFailed
                )
            },
            onError = { e ->
                lError("An exception was encountered while performing the refresh task.", e)
                onFailed(e)
            }
        ).apply { updateMessage(R.string.account_other_login_select_role_logging, account.username) }

        TaskSystem.submitTask(task)
    }
}
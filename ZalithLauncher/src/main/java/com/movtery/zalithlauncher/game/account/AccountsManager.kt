package com.movtery.zalithlauncher.game.account

import android.content.Context
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.database.AppDatabase
import com.movtery.zalithlauncher.game.account.auth_server.data.AuthServer
import com.movtery.zalithlauncher.game.account.auth_server.data.AuthServerDao
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.apache.commons.io.FileUtils
import java.util.concurrent.CopyOnWriteArrayList

object AccountsManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    //账号相关
    private val _accounts = CopyOnWriteArrayList<Account>()
    private val _accountsFlow = MutableStateFlow<List<Account>>(emptyList())
    val accountsFlow: StateFlow<List<Account>> = _accountsFlow

    private val _currentAccountFlow = MutableStateFlow<Account?>(null)
    val currentAccountFlow: StateFlow<Account?> = _currentAccountFlow

    //认证服务器
    private val _authServers = CopyOnWriteArrayList<AuthServer>()
    private val _authServersFlow = MutableStateFlow<List<AuthServer>>(emptyList())
    val authServersFlow: StateFlow<List<AuthServer>> = _authServersFlow

    private lateinit var database: AppDatabase
    private lateinit var accountDao: AccountDao
    private lateinit var authServerDao: AuthServerDao

    /**
     * 初始化整个账号系统
     */
    fun initialize(context: Context) {
        database = AppDatabase.getInstance(context)
        accountDao = database.accountDao()
        authServerDao = database.authServerDao()
    }

    /**
     * 刷新当前已登录的账号，已登录的账号保存在数据库中
     */
    fun reloadAccounts() {
        scope.launch {
            val loadedAccounts = accountDao.getAllAccounts()
            _accounts.clear()
            _accounts.addAll(loadedAccounts)

            _accounts.sortWith(compareBy<Account>(
                { it.accountTypePriority() },
                { it.username },
            ))
            _accountsFlow.value = _accounts.toList()

            if (_accounts.isNotEmpty() && !isAccountExists(AllSettings.currentAccount.getValue())) {
                setCurrentAccount(_accounts[0])
            }

            refreshCurrentAccountState()

            lInfo("Loaded ${_accounts.size} accounts")
        }
    }

    /**
     * 刷新当前已保存的认证服务器，认证服务器保存在数据库中
     */
    fun reloadAuthServers() {
        scope.launch {
            val loadedServers = authServerDao.getAllServers()
            _authServers.clear()
            _authServers.addAll(loadedServers)

            _authServers.sortWith { o1, o2 -> o1.serverName.compareTo(o2.serverName) }
            _authServersFlow.value = _authServers.toList()

            lInfo("Loaded ${_authServers.size} auth servers")
        }
    }

    /**
     * 执行登陆操作
     */
    fun performLogin(
        context: Context,
        account: Account,
        onSuccess: suspend (Account, task: Task) -> Unit = { _, _ -> },
        onFailed: (th: Throwable) -> Unit = {}
    ) {
        val task = performLoginTask(context, account, onSuccess, onFailed)
        task?.let { TaskSystem.submitTask(it) }
    }

    /**
     * 获取登陆操作的任务对象
     */
    fun performLoginTask(
        context: Context,
        account: Account,
        onSuccess: suspend (Account, task: Task) -> Unit = { _, _ -> },
        onFailed: (th: Throwable) -> Unit = {},
        onFinally: () -> Unit = {}
    ): Task? =
        when {
            account.isNoLoginRequired() -> null
            account.isAuthServerAccount() -> {
                otherLogin(context = context, account = account, onSuccess = onSuccess, onFailed = onFailed, onFinally = onFinally)
            }
            account.isMicrosoftAccount() -> {
                microsoftRefresh(account = account, onSuccess = onSuccess, onFailed = onFailed, onFinally = onFinally)
            }
            else -> null
        }

    /**
     * 获取当前已登录的账号
     */
    fun getCurrentAccount(): Account? {
        return _accounts.find {
            it.uniqueUUID == AllSettings.currentAccount.getValue()
        } ?: _accounts.firstOrNull()
    }

    /**
     * 设置并保存当前账号
     */
    fun setCurrentAccount(account: Account) {
        AllSettings.currentAccount.put(account.uniqueUUID).save()
        refreshCurrentAccountState()
    }

    private fun refreshCurrentAccountState() {
        _currentAccountFlow.value = getCurrentAccount()
    }

    /**
     * 保存账号到数据库
     */
    fun saveAccount(account: Account) {
        scope.launch {
            suspendSaveAccount(account)
        }
    }

    /**
     * 保存账号到数据库
     */
    suspend fun suspendSaveAccount(account: Account) {
        runCatching {
            accountDao.saveAccount(account)
            lInfo("Saved account: ${account.username}")
        }.onFailure { e ->
            lError("Failed to save account: ${account.username}", e)
        }
        reloadAccounts()
    }

    /**
     * 从数据库中删除账号，并刷新
     */
    fun deleteAccount(account: Account) {
        scope.launch {
            accountDao.deleteAccount(account)
            val skinFile = account.getSkinFile()
            FileUtils.deleteQuietly(skinFile)
            reloadAccounts()
        }
    }

    /**
     * 保存认证服务器到数据库
     */
    suspend fun saveAuthServer(server: AuthServer) {
        runCatching {
            authServerDao.saveServer(server)
            lInfo("Saved auth server: ${server.serverName} -> ${server.baseUrl}")
        }.onFailure { e ->
            lError("Failed to save auth server: ${server.serverName}", e)
        }
        reloadAuthServers()
    }

    /**
     * 从数据库中删除认证服务器，并刷新
     */
    fun deleteAuthServer(server: AuthServer) {
        scope.launch {
            authServerDao.deleteServer(server)
            reloadAuthServers()
        }
    }

    /**
     * 是否已登录过微软账号
     */
    fun hasMicrosoftAccount(): Boolean = _accounts.any { it.isMicrosoftAccount() }

    /**
     * 通过账号的profileId读取账号
     */
    fun loadFromProfileID(profileId: String): Account? =
        _accounts.find { it.profileId == profileId }

    /**
     * 账号是否存在
     */
    fun isAccountExists(uniqueUUID: String): Boolean {
        return uniqueUUID.isNotEmpty() && _accounts.any { it.uniqueUUID == uniqueUUID }
    }

    /**
     * 认证服务器是否存在
     */
    fun isAuthServerExists(baseUrl: String): Boolean {
        return baseUrl.isNotEmpty() && _authServers.any { it.baseUrl == baseUrl }
    }
}

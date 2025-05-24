package com.movtery.zalithlauncher.game.account

import android.content.Context
import android.util.Log
import com.google.gson.JsonSyntaxException
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.CryptoManager
import com.movtery.zalithlauncher.utils.GSON
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

object AccountsManager {
    private val accountsLock = Any()
    private val _accounts = CopyOnWriteArrayList<Account>()
    private val _accountsFlow = MutableStateFlow<List<Account>>(emptyList())
    val accountsFlow: StateFlow<List<Account>> = _accountsFlow

    private val _currentAccountFlow = MutableStateFlow<Account?>(null)
    val currentAccountFlow: StateFlow<Account?> = _currentAccountFlow

    /**
     * 刷新当前已登录的账号，已登录的账号保存在 `PathManager.DIR_ACCOUNT` 目录中
     */
    fun reloadAccounts() {
        synchronized(accountsLock) {
            _accounts.clear()

            val accountFiles = PathManager.DIR_ACCOUNT.takeIf { it.exists() && it.isDirectory }?.listFiles { it -> it.isFile } ?: emptyArray()
            val loadedAccounts = mutableListOf<Account>()

            accountFiles.forEach { file ->
                runCatching {
                    parseAccount(file)
                }.onSuccess { account ->
                    if (!loadedAccounts.contains(account)) {
                        loadedAccounts.add(account)
                    }
                }.onFailure { e ->
                    Log.e("AccountsManager", "Failed to load account from ${file.name}", e)
                }
            }

            _accounts.addAll(loadedAccounts)

            var isCurrentAccountRefreshed = false

            if (_accounts.isNotEmpty() && !isAccountExists(AllSettings.currentAccount.getValue())) {
                setCurrentAccount(_accounts[0])
                isCurrentAccountRefreshed = true
            }

            if (!isCurrentAccountRefreshed) {
                refreshCurrentAccountState()
            }

            _accounts.sortWith { o1, o2 -> o1.username.compareTo(o2.username) }
            _accountsFlow.value = _accounts.toList()

            Log.i("AccountsManager", "Loaded ${_accounts.size} accounts")
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
            account.isOtherLoginAccount() -> {
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
        return synchronized(accountsLock) {
            loadFromUniqueUUID(AllSettings.currentAccount.getValue())
                ?: _accounts.firstOrNull()
        }
    }

    /**
     * 设置并保存当前账号
     */
    fun setCurrentAccount(account: Account) {
        AllSettings.currentAccount.put(account.uniqueUUID).save()
        refreshCurrentAccountState()
    }

    /**
     * 设置并保存当前账号
     */
    fun setCurrentAccount(uniqueUUID: String) {
        AllSettings.currentAccount.put(uniqueUUID).save()
        refreshCurrentAccountState()
    }

    private fun refreshCurrentAccountState() {
        _currentAccountFlow.value = getCurrentAccount()
    }

    /**
     * 移除账号
     */
    fun deleteAccount(account: Account) {
        val accountFile = File(PathManager.DIR_ACCOUNT, account.uniqueUUID)
        val accountSkinFile = account.getSkinFile()
        FileUtils.deleteQuietly(accountFile)
        FileUtils.deleteQuietly(accountSkinFile)
        reloadAccounts()
    }

    /**
     * 是否已登录过微软账号
     */
    fun hasMicrosoftAccount(): Boolean = synchronized(accountsLock) {
        _accounts.any { it.isMicrosoftAccount() }
    }

    /**
     * 通过账号信息保存的文件读取账号
     */
    @Throws(JsonSyntaxException::class)
    fun parseAccount(accountFile: File): Account {
        return parseAccount(accountFile.readText())
    }

    /**
     * 通过账号信息字符串读取账号
     */
    @Throws(JsonSyntaxException::class)
    fun parseAccount(rawJson: String): Account {
        return GSON.fromJson(rawJson, Account::class.java)
    }

    /**
     * 通过账号的唯一标识符读取账号
     */
    fun loadFromUniqueUUID(uniqueUUID: String): Account? {
        if (!isAccountExists(uniqueUUID)) return null
        return try {
            parseAccount(File(PathManager.DIR_ACCOUNT, uniqueUUID))
        } catch (e: IOException) {
            Log.e("AccountsManager", "Caught an exception while loading the profile", e)
            null
        } catch (e: JsonSyntaxException) {
            Log.e("AccountsManager", "Caught an exception while loading the profile", e)
            null
        }
    }

    /**
     * 通过账号的profileId读取账号
     */
    fun loadFromProfileID(profileId: String): Account? =
        _accounts.find { it.profileId == profileId }

    /**
     * 账号是否存在
     */
    fun isAccountExists(uniqueUUID: String): Boolean {
        return uniqueUUID.isNotEmpty() && File(PathManager.DIR_ACCOUNT, uniqueUUID).exists()
    }
}

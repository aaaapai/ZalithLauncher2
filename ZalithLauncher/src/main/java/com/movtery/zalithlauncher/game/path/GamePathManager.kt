package com.movtery.zalithlauncher.game.path

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings.Companion.currentGamePathId
import com.movtery.zalithlauncher.utils.CryptoManager
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.StoragePermissionsUtils.Companion.checkPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID

/**
 * 游戏目录管理，为支持将游戏文件保存至不同的路径
 */
object GamePathManager {
    private val pathConfig = File(PathManager.DIR_GAME, "game_path_config.json")
    private val defaultGamePath = File(PathManager.DIR_FILES_EXTERNAL, ".minecraft").absolutePath

    /**
     * 默认游戏目录的ID
     */
    const val DEFAULT_ID = "default"

    private val _gamePathData = MutableStateFlow<List<GamePathItem>>(listOf())
    val gamePathData: StateFlow<List<GamePathItem>> = _gamePathData

    /**
     * 当前选择的路径
     */
    var currentPath by mutableStateOf<String>(defaultGamePath)

    /**
     * 当前用户路径
     */
    fun getUserHome(): String = File(currentPath).parentFile!!.absolutePath

    fun reloadPath() {
        _gamePathData.update { emptyList() }

        val newValue = mutableListOf<GamePathItem>()
        //添加默认游戏目录
        newValue.add(0, GamePathItem(DEFAULT_ID, "", defaultGamePath))

        run parseConfig@{
            if (pathConfig.exists()) {
                val rawString = pathConfig.readText().takeIf { it.isNotEmpty() } ?: return@parseConfig
                val configString = CryptoManager.decrypt(rawString)
                parsePathConfig(configString).takeIf { it.isNotEmpty() }?.let {
                    newValue.addAll(it)
                }
            }
        }

        _gamePathData.update { it + newValue }

        if (!checkPermissions()) {
            currentPath = defaultGamePath
        } else {
            refreshCurrentPath()
        }
    }

    private fun String.createNoMediaFile() {
        val noMediaFile = File(this, ".nomedia")
        if (!noMediaFile.exists()) {
            runCatching {
                noMediaFile.createNewFile()
            }.onFailure { e ->
                Log.e("GamePathManager", "Failed to create .nomedia file in $this", e)
            }
        }
    }

    /**
     * 查找是否存在指定id的项
     */
    fun containsId(id: String): Boolean = _gamePathData.value.any { it.id == id }

    /**
     * 查找是否存在指定path的项
     */
    fun containsPath(path: String): Boolean = _gamePathData.value.any { it.path == path }

    /**
     * 修改并保存指定id项的标题
     * @throws IllegalArgumentException 未找到匹配项
     */
    fun modifyTitle(id: String, modifiedTitle: String) {
        _gamePathData.update { currentList ->
            currentList.toMutableList().apply {
                val index = indexOfFirst { it.id == id }.takeIf { it > 0 }
                    ?: throw IllegalArgumentException("Item with ID $id not found, unable to rename.")
                val item = get(index)
                set(index, GamePathItem(id, modifiedTitle, item.path))
            }
        }
        saveConfig()
    }

    /**
     * 添加新的路径并保存
     * @throws IllegalArgumentException 当前添加的路径与现有项冲突
     */
    fun addNewPath(title: String, path: String) {
        if (containsPath(path)) throw IllegalArgumentException("The path conflicts with an existing item!")
        _gamePathData.update { currentList ->
            currentList + GamePathItem(id = generateUUID(), title = title, path = path)
        }
        saveConfig()
    }

    /**
     * 删除路径并保存
     */
    fun removePath(id: String) {
        if (!containsId(id)) return
        val item = _gamePathData.value.find { it.id == id } ?: return
        _gamePathData.update { currentList ->
            currentList - item
        }
        refreshCurrentPath()
        saveConfig()
    }

    /**
     * 保存为默认的游戏目录
     */
    fun saveDefaultPath() {
        saveCurrentPathUncheck(DEFAULT_ID)
    }

    /**
     * 保存当前选择的路径
     * @throws IllegalStateException 未授予存储/管理所有文件权限
     * @throws IllegalArgumentException 未找到匹配项
     */
    fun saveCurrentPath(id: String) {
        if (!checkPermissions()) throw IllegalStateException("Storage permissions are not granted")
        if (!containsId(id)) throw IllegalArgumentException("No match found!")
        saveCurrentPathUncheck(id)
    }

    private fun saveCurrentPathUncheck(id: String) {
        if (currentGamePathId.getValue() == id) return
        currentGamePathId.put(id).save()
        refreshCurrentPath()
    }

    private fun refreshCurrentPath() {
        val id = currentGamePathId.getValue()
        _gamePathData.value.find { it.id == id }?.let { item ->
            currentPath = item.path
            currentPath.createNoMediaFile()
            VersionsManager.refresh()
        } ?: saveCurrentPath(DEFAULT_ID)
    }

    private fun generateUUID(): String {
        val uuid = UUID.randomUUID().toString()
        return if (containsId(uuid)) generateUUID()
        else uuid
    }

    private fun parsePathConfig(configString: rawConfig): List<GamePathItem> {
        return runCatching {
            GSON.fromJson(configString, Array<GamePathItem>::class.java).toList()
        }.getOrElse { e ->
            Log.e("GamePathManager", "Failed to parse game path config!", e)
            emptyList()
        }
    }

    private fun saveConfig() {
        val filteredData = _gamePathData.value.filter { it.id != DEFAULT_ID }
        val rawConfig = GSON.toJson(filteredData)
        val string = CryptoManager.encrypt(rawConfig)
        runCatching {
            pathConfig.writeText(rawConfig)
        }.onFailure { e ->
            Log.e("GamePathManager", "Failed to save game path config!", e)
        }
    }
}

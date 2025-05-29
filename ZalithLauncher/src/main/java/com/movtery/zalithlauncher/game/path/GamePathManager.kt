package com.movtery.zalithlauncher.game.path

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.database.AppDatabase
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings.Companion.currentGamePathId
import com.movtery.zalithlauncher.utils.StoragePermissionsUtils.Companion.checkPermissions
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * 游戏目录管理，为支持将游戏文件保存至不同的路径
 */
object GamePathManager {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val defaultGamePath = File(PathManager.DIR_FILES_EXTERNAL, ".minecraft").absolutePath
    /**
     * 默认游戏目录的ID
     */
    const val DEFAULT_ID = "default"

    private val _gamePathData = MutableStateFlow<List<GamePath>>(listOf())
    val gamePathData: StateFlow<List<GamePath>> = _gamePathData

    /**
     * 当前选择的路径
     */
    var currentPath by mutableStateOf<String>(defaultGamePath)

    /**
     * 当前用户路径
     */
    fun getUserHome(): String = File(currentPath).parentFile!!.absolutePath

    private lateinit var database: AppDatabase
    private lateinit var gamePathDao: GamePathDao

    fun initialize(context: Context) {
        database = AppDatabase.getInstance(context)
        gamePathDao = database.gamePathDao()
    }

    fun reloadPath() {
        scope.launch {
            _gamePathData.update { emptyList() }

            val newValue = mutableListOf<GamePath>()
            //添加默认游戏目录
            newValue.add(0, GamePath(DEFAULT_ID, "", defaultGamePath))

            run parseConfig@{
                //从数据库中加载游戏目录
                val paths = gamePathDao.getAllPaths()
                newValue.addAll(paths.sortedBy { it.title })
            }

            _gamePathData.update { newValue }

            if (!checkPermissions()) {
                currentPath = defaultGamePath
            } else {
                refreshCurrentPath()
            }

            lInfo("Loaded ${_gamePathData.value.size} game paths")
        }
    }

    private fun String.createNoMediaFile() {
        val noMediaFile = File(this, ".nomedia")
        if (!noMediaFile.exists()) {
            runCatching {
                noMediaFile.createNewFile()
            }.onFailure { e ->
                lError("Failed to create .nomedia file in $this", e)
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
     * 修改并保存指定目录的标题
     * @throws IllegalArgumentException 未找到匹配项
     */
    fun modifyTitle(path: GamePath, modifiedTitle: String) {
        if (!containsId(path.id)) throw IllegalArgumentException("Item with ID ${path.id} not found, unable to rename.")
        path.title = modifiedTitle
        savePath(path)
    }

    /**
     * 添加新的路径并保存
     * @throws IllegalArgumentException 当前添加的路径与现有项冲突
     */
    fun addNewPath(title: String, path: String) {
        if (containsPath(path)) throw IllegalArgumentException("The path conflicts with an existing item!")
        savePath(
            GamePath(id = generateUUID(), title = title, path = path)
        )
    }

    /**
     * 删除路径并保存
     */
    fun removePath(path: GamePath) {
        if (!containsId(path.id)) return
        deletePath(path)
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
            if (currentPath == item.path) return //避免重复刷新
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

    private fun savePath(path: GamePath) {
        scope.launch {
            runCatching {
                gamePathDao.savePath(path)
                lInfo("Saved game path: ${path.path}")
            }.onFailure { e ->
                lError("Failed to save game path config!", e)
            }
            reloadPath()
        }
    }

    private fun deletePath(path: GamePath) {
        scope.launch {
            gamePathDao.deletePath(path)
            reloadPath()
        }
    }
}

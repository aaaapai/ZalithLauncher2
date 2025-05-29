package com.movtery.zalithlauncher.ui.screens.content.versions.elements

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.nbt.asBoolean
import com.movtery.zalithlauncher.utils.nbt.asCompoundTag
import com.movtery.zalithlauncher.utils.nbt.asInt
import com.movtery.zalithlauncher.utils.nbt.asLong
import com.movtery.zalithlauncher.utils.nbt.asString
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.stripColorCodes
import com.movtery.zalithlauncher.utils.string.isBiggerOrEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.querz.nbt.io.NBTUtil
import net.querz.nbt.tag.CompoundTag
import org.apache.commons.io.FileUtils
import java.io.File

/** 存档加载状态 */
sealed interface SavesState {
    data object None : SavesState
    /** 正在加载所有存档 */
    data object Loading : SavesState
}

sealed interface SavesOperation {
    data object None : SavesOperation
    /** 执行任务中 */
    data object Progress : SavesOperation
    /** 快速启动 */
    data class QuickPlay(val saveData: SaveData) : SavesOperation
    /** 重命名存档输入对话框 */
    data class RenameSave(val saveData: SaveData) : SavesOperation
    /** 备份存档输入对话框 */
    data class BackupSave(val saveData: SaveData) : SavesOperation
    /** 删除存档对话框 */
    data class DeleteSave(val saveData: SaveData) : SavesOperation
}

/**
 * 存档过滤器
 */
data class SavesFilter(val onlyShowCompatible: Boolean, val saveName: String = "")

/**
 * 简易过滤器，过滤特定的存档
 * @param minecraftVersion 当前 MC 的版本，用于比较版本兼容性
 * @param savesFilter 存档过滤器
 */
fun List<SaveData>.filterSaves(
    minecraftVersion: String,
    savesFilter: SavesFilter
) = this.filter {
    val isCompatible = !savesFilter.onlyShowCompatible || it.isCompatible(minecraftVersion)

    val nameMatches = savesFilter.saveName.isEmpty() ||
            //存档名、存档文件夹名均可参与搜索
            //自动过滤掉颜色占位符
            it.levelName?.stripColorCodes()?.contains(savesFilter.saveName, true) == true ||
            it.saveFile.name.stripColorCodes().contains(savesFilter.saveName, true)

    isCompatible && nameMatches
}

/**
 * 判断这个存档是否与指定的版本兼容
 * @param minecraftVersion 当前 MC 的版本，用于比较版本兼容性
 */
fun SaveData.isCompatible(minecraftVersion: String) =
    isValid && levelMCVersion != null && minecraftVersion.isBiggerOrEqualTo(levelMCVersion)

/**
 * 存档解析后的信息类
 */
data class SaveData(
    /** 存档文件夹 */
    val saveFile: File,
    /** 提前计算好的存档大小 */
    val saveSize: Long,
    /** 该存档是否有效 */
    val isValid: Boolean,
    /** 存档真正的名字 */
    val levelName: String? = null,
    /** 游戏的版本名称 */
    val levelMCVersion: String? = null,
    /** 存档游戏模式 */
    val gameMode: GameMode? = null,
    /** 存档难度等级 */
    val difficulty: Difficulty? = null,
    /** 难度是否被锁定 */
    val difficultyLocked: Boolean? = null,
    /** 是否为极限模式 */
    val hardcoreMode: Boolean? = null,
    /** 存档是否启用命令(作弊) */
    val allowCommands: Boolean? = null,
    /** 世界种子 */
    val worldSeed: Long? = null
)

/**
 * @param levelCode 在 level.dat 中存储的值
 */
enum class GameMode(val levelCode: Int, val nameRes: Int) {
    /** 生存模式 */
    SURVIVAL(0, R.string.saves_manage_gamemode_survival),
    /** 创造模式 */
    CREATIVE(1, R.string.saves_manage_gamemode_creative),
    /** 冒险模式 */
    ADVENTURE(2, R.string.saves_manage_gamemode_adventure),
    /** 旁观模式 */
    SPECTATOR(3, R.string.saves_manage_gamemode_spectator)
}

/**
 * @param levelCode 在 level.dat 中存储的值
 */
enum class Difficulty(val levelCode: Int, val nameRes: Int) {
    /** 和平 */
    PEACEFUL(0, R.string.saves_manage_difficulty_peaceful),
    /** 简单 */
    EASY(1, R.string.saves_manage_difficulty_easy),
    /** 普通 */
    NORMAL(2, R.string.saves_manage_difficulty_normal),
    /** 困难 */
    HARD(3, R.string.saves_manage_difficulty_hard)
}

/**
 * 从 level.dat 文件中解析出必要的信息，构建 SaveData
 * [参考 Minecraft Wiki](https://zh.minecraft.wiki/w/%E5%AD%98%E6%A1%A3%E5%9F%BA%E7%A1%80%E6%95%B0%E6%8D%AE%E5%AD%98%E5%82%A8%E6%A0%BC%E5%BC%8F#%E5%AD%98%E5%82%A8%E6%A0%BC%E5%BC%8F)
 * @param saveFile 存档的文件夹
 * @param levelDatFile level.dat 文件
 */
suspend fun parseLevelDatFile(saveFile: File, levelDatFile: File): SaveData = withContext(Dispatchers.IO) {
    val fileSize = FileUtils.sizeOf(saveFile)
    runCatching {
        if (!levelDatFile.exists()) error("The ${levelDatFile.absolutePath} file does not exist!")

        val compound: CompoundTag = NBTUtil.read(levelDatFile, true).tag as? CompoundTag
            ?: error("Failed to read the level.dat file as a CompoundTag.")
        val data: CompoundTag = compound.asCompoundTag("Data")
            ?: error("Data entry not found in the NBT structure tree.")

        //存档名称，不存在则为空
        val levelName = data.asString("LevelName", "")
        //存档的游戏版本
        val levelMCVersion = data.asCompoundTag("Version")?.asString("Name", null)
        //存档的游戏模式
        val gameMode = data.asInt("GameType", 0) //默认为生存模式
            ?.let { levelCode -> GameMode.entries.find { it.levelCode == levelCode } }
        //游戏难度
        val difficulty = data.asInt("Difficulty", 2) //默认为普通
            ?.let { levelCode -> Difficulty.entries.find { it.levelCode == levelCode } }
        //是否锁定了游戏难度
        val difficultyLocked = data.asBoolean("DifficultyLocked", false)
        //是否为极限模式
        val hardcoreMode = data.asBoolean("hardcore", false)
        //是否开启了命令（作弊）
        val allowCommands = if (data.containsKey("allowCommands")) {
            data.asBoolean("allowCommands", false)
        } else {
            //如果不存在 allowCommands，则通过游戏模式判断
            gameMode == GameMode.CREATIVE
        }
        //世界种子
        val worldSeed = data.asCompoundTag("WorldGenSettings")
            ?.asLong("seed", null)
            //如果不存在，则尝试获取 RandomSeed
            ?: data.asLong("RandomSeed", null)

        SaveData(
            saveFile = saveFile,
            saveSize = fileSize,
            isValid = true,
            levelName = levelName,
            levelMCVersion = levelMCVersion,
            gameMode = gameMode,
            //关于极限模式：极限模式开启后，难度会被锁定为困难（尽管 level.dat 文件内并不会这样存储）
            //https://zh.minecraft.wiki/w/%E6%9E%81%E9%99%90%E6%A8%A1%E5%BC%8F#%E5%88%9B%E5%BB%BA%E6%96%B0%E7%9A%84%E4%B8%96%E7%95%8C
            difficulty = if (hardcoreMode) Difficulty.HARD else difficulty,
            difficultyLocked = difficultyLocked,
            hardcoreMode = hardcoreMode,
            allowCommands = allowCommands,
            worldSeed = worldSeed
        )
    }.onFailure {
        lWarning("An exception occurred while reading and parsing the level.dat file (${levelDatFile.absolutePath}).", it)
    }.getOrElse {
        //读取出现异常，返回一个无效数据
        SaveData(
            saveFile = saveFile,
            saveSize = fileSize,
            isValid = false
        )
    }
}
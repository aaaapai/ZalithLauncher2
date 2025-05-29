package com.movtery.zalithlauncher.game.version.installed

import com.google.gson.annotations.SerializedName
import com.movtery.zalithlauncher.game.path.getGameHome
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 当前游戏状态信息（支持旧配置迁移）
 * @property version 当前选择的版本名称
 * @property favoritesMap 收藏夹映射表 <收藏夹名称, 包含的版本集合>
 */
data class CurrentGameInfo(
    @SerializedName("version")
    var version: String = "",
    @SerializedName("favoritesInfo")
    val favoritesMap: MutableMap<String, MutableSet<String>> = ConcurrentHashMap()
) {
    /**
     * 原子化保存当前状态到文件
     */
    fun saveCurrentInfo() {
        val infoFile = getInfoFile()
        runCatching {
            FileUtils.writeByteArrayToFile(infoFile, GSON.toJson(this).toByteArray(Charsets.UTF_8))
        }.onFailure { e ->
            lError("Save failed: ${infoFile.absolutePath}", e)
        }
    }

    companion object {
        private fun getInfoFile() = File(getGameHome(), "zalith-game.cfg")

        /**
         * 刷新并返回最新的游戏信息（自动处理旧配置迁移）
         */
        fun refreshCurrentInfo(): CurrentGameInfo {
            val infoFile = getInfoFile()

            return runCatching {
                when {
                    infoFile.exists() -> loadFromJsonFile(infoFile)
                    else -> createNewConfig()
                }
            }.getOrElse { e ->
                lError("Refresh failed", e)
                createNewConfig()
            }
        }

        private fun loadFromJsonFile(infoFile: File): CurrentGameInfo {
            return GSON.fromJson(infoFile.readText(), CurrentGameInfo::class.java).also { info ->
                checkNotNull(info) { "Deserialization returned null" }
            }
        }

        private fun createNewConfig() = CurrentGameInfo().applyPostActions()

        private fun CurrentGameInfo.applyPostActions(): CurrentGameInfo {
            saveCurrentInfo()
            return this
        }
    }
}
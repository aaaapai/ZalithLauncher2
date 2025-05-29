package com.movtery.zalithlauncher.game.path

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 游戏目录
 */
@Entity(tableName = "gamePaths")
data class GamePath(
    /**
     * 单项唯一ID
     */
    @PrimaryKey
    val id: String,
    /**
     * 游戏目录的标题
     */
    var title: String,
    /**
     * 目标路径
     */
    val path: String
)

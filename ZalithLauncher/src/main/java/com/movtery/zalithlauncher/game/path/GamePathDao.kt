package com.movtery.zalithlauncher.game.path

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GamePathDao {
    @Query("SELECT * FROM gamePaths")
    suspend fun getAllPaths(): List<GamePath>

    @Query("SELECT * FROM gamePaths WHERE id = :id")
    suspend fun getPath(id: String): GamePath?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePath(path: GamePath)

    @Delete
    suspend fun deletePath(path: GamePath)
}
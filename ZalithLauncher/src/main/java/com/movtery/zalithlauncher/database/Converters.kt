package com.movtery.zalithlauncher.database

import androidx.room.TypeConverter
import com.movtery.zalithlauncher.game.skin.SkinModelType

class Converters {
    @TypeConverter
    fun fromSkinModelType(type: SkinModelType): String = type.name

    @TypeConverter
    fun toSkinModelType(value: String): SkinModelType =
        enumValueOf(value)
}
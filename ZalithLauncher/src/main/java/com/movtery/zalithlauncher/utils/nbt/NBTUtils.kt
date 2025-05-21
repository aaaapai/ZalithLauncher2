package com.movtery.zalithlauncher.utils.nbt

import net.querz.nbt.tag.ByteTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.DoubleTag
import net.querz.nbt.tag.FloatTag
import net.querz.nbt.tag.IntTag
import net.querz.nbt.tag.LongTag
import net.querz.nbt.tag.ShortTag
import net.querz.nbt.tag.StringTag
import net.querz.nbt.tag.Tag

private inline fun <reified T : Tag<*>, R> CompoundTag.getAs(key: String, crossinline mapper: (T) -> R, defaultValue: R): R {
    return (this.get(key) as? T)?.let(mapper) ?: defaultValue
}

/** 获取指定键对应的 CompoundTag */
fun CompoundTag.asCompoundTag(key: String): CompoundTag? {
    return this.get(key) as? CompoundTag
}

/**
 * 获取指定键对应的 Boolean 值，通过 ByteTag 的方式手动判断
 */
fun CompoundTag.asBoolean(key: String, defaultValue: Boolean): Boolean {
    return asByte(key, if (defaultValue) 1 else 0) == 1.toByte()
}

/** 获取指定键对应的 Byte 值 */
fun CompoundTag.asByte(key: String, defaultValue: Byte?): Byte? {
    return getAs<ByteTag, Byte?>(key, { it.asByte() }, defaultValue)
}

/** 获取指定键对应的 Short 值 */
fun CompoundTag.asShort(key: String, defaultValue: Short?): Short? {
    return getAs<ShortTag, Short?>(key, { it.asShort() }, defaultValue)
}

/** 获取指定键对应的 Int 值 */
fun CompoundTag.asInt(key: String, defaultValue: Int?): Int? {
    return getAs<IntTag, Int?>(key, { it.asInt() }, defaultValue)
}

/** 获取指定键对应的 Long 值 */
fun CompoundTag.asLong(key: String, defaultValue: Long?): Long? {
    return getAs<LongTag, Long?>(key, { it.asLong() }, defaultValue)
}

/** 获取指定键对应的 Float 值 */
fun CompoundTag.asFloat(key: String, defaultValue: Float?): Float? {
    return getAs<FloatTag, Float?>(key, { it.asFloat() }, defaultValue)
}

/** 获取指定键对应的 Double 值 */
fun CompoundTag.asDouble(key: String, defaultValue: Double?): Double? {
    return getAs<DoubleTag, Double?>(key, { it.asDouble() }, defaultValue)
}

/** 获取指定键对应的 String 值 */
fun CompoundTag.asString(key: String, defaultValue: String?): String? {
    return getAs<StringTag, String?>(key, { it.value }, defaultValue)
}

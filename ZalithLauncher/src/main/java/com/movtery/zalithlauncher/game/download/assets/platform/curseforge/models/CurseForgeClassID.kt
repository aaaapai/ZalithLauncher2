package com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * CurseForge 资源搜索类别
 */
@Serializable(with = CurseForgeClassID.Serializer::class)
enum class CurseForgeClassID(val classID: Int, val slug: String) {
    /** 模组 */
    MOD(6, "mc-mods"),

    /** 整合包 */
    MOD_PACK(4471, "modpacks"),

    /** 资源包 */
    RESOURCE_PACK(12, "texture-packs"),

    /** 存档 */
    SAVES(17, "worlds"),

    /** 光影包 */
    SHADERS(6552, "shaders");

    companion object {
        private val map = entries.associateBy { it.classID }
        fun fromId(id: Int): CurseForgeClassID = map[id] ?: error("Unknown class ID: $id")
    }

    object Serializer : KSerializer<CurseForgeClassID> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("CurseForgeClassID", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): CurseForgeClassID {
            val id = decoder.decodeInt()
            return CurseForgeClassID.fromId(id)
        }

        override fun serialize(encoder: Encoder, value: CurseForgeClassID) {
            encoder.encodeInt(value.classID)
        }
    }
}
package com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.ModLoaderDisplayLabel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = CurseForgeModLoader.Serializer::class)
enum class CurseForgeModLoader(val code: Int) : ModLoaderDisplayLabel {
    ANY(0) {
        override fun getDisplayName(): String = ""
    },
    FORGE(1) {
        override fun getDisplayName(): String = ModLoader.FORGE.displayName
    },
    CAULDRON(2) {
        override fun getDisplayName(): String = "Cauldron"
    },
    LITE_LOADER(3) {
        override fun getDisplayName(): String = "LiteLoader"
    },
    FABRIC(4) {
        override fun getDisplayName(): String = ModLoader.FABRIC.displayName
    },
    QUILT(5) {
        override fun getDisplayName(): String = ModLoader.QUILT.displayName
    },
    NEOFORGE(6) {
        override fun getDisplayName(): String = ModLoader.NEOFORGE.displayName
    };

    override fun index(): Int = this.ordinal

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
    }

    companion object CREATOR: Parcelable.Creator<CurseForgeModLoader> {
        private val map = entries.associateBy { it.code }
        fun fromCode(code: Int): CurseForgeModLoader = map[code] ?: error("Unknown mod loader code: $code")

        override fun createFromParcel(parcel: Parcel): CurseForgeModLoader {
            return CurseForgeModLoader.valueOf(
                value = parcel.readString()!!
            )
        }

        override fun newArray(size: Int): Array<out CurseForgeModLoader?> {
            return arrayOfNulls(size)
        }
    }

    object Serializer : KSerializer<CurseForgeModLoader> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("CurseForgeModLoader", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): CurseForgeModLoader {
            val code = decoder.decodeInt()
            return CurseForgeModLoader.fromCode(code)
        }

        override fun serialize(encoder: Encoder, value: CurseForgeModLoader) {
            encoder.encodeInt(value.code)
        }
    }
}

/**
 * 可视化筛选器支持的模组加载器
 */
val curseForgeModLoaderFilters = listOf(
    CurseForgeModLoader.FORGE,
    CurseForgeModLoader.FABRIC,
    CurseForgeModLoader.NEOFORGE,
    CurseForgeModLoader.QUILT
)
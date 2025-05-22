package com.movtery.zalithlauncher.game.version.installed

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.utils.getInt
import com.movtery.zalithlauncher.utils.toBoolean

class VersionInfo(
    val minecraftVersion: String,
    val quickPlay: QuickPlay,
    val loaderInfo: LoaderInfo?
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readParcelable(QuickPlay::class.java.classLoader)!!,
        parcel.readParcelable(LoaderInfo::class.java.classLoader)
    )

    /**
     * 拼接Minecraft的版本信息，包括ModLoader信息
     * @return 用", "分割的信息字符串
     */
    fun getInfoString(): String {
        val infoList = mutableListOf<String>().apply {
            add(minecraftVersion)
            loaderInfo?.let { info ->
                when {
                    info.name.isNotBlank() && info.version.isNotBlank() -> add("${info.name} - ${info.version}")
                    info.name.isNotBlank() -> add(info.name)
                    info.version.isNotBlank() -> add(info.version)
                }
            }
        }
        return infoList.joinToString(", ")
    }

    /**
     * [Reference PCL2](https://github.com/Hex-Dragon/PCL2/blob/dc611a982f8f97fab2c4275d1176db484f8549a4/Plain%20Craft%20Launcher%202/Modules/Minecraft/ModMinecraft.vb#L426-L438)
     */
    fun getMcVersionCode(): McVersionCode {
        return when {
            minecraftVersion.contains("w") || minecraftVersion.equals("pending", ignoreCase = true) -> {
                //快照或未发布版本，特殊处理
                McVersionCode(99, 99)
            }

            minecraftVersion.startsWith("1.") -> {
                val parts = minecraftVersion.split(" ", "_", "-", ".")
                val main = parts.getOrNull(1)?.takeIf { it.length <= 2 }?.toIntOrNull() ?: 0
                val sub = parts.getOrNull(2)?.takeIf { it.length <= 2 }?.toIntOrNull() ?: 0
                McVersionCode(main, sub)
            }

            else -> {
                McVersionCode(0, 0)
            }
        }
    }

    data class McVersionCode(
        val main: Int,
        val sub: Int
    )

    data class LoaderInfo(
        val name: String,
        val version: String
    ): Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readString() ?: ""
        )

        /**
         * 通过加载器名称，获得对应的环境变量键名
         */
        fun getLoaderEnvKey(): String? {
            return when(name) {
                "OptiFine" -> "INST_OPTIFINE"
                "Forge" -> "INST_FORGE"
                "NeoForge" -> "INST_NEOFORGE"
                "Fabric" -> "INST_FABRIC"
                "Quilt" -> "INST_QUILT"
                "LiteLoader" -> "INST_LITELOADER"
                else -> null
            }
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(name)
            dest.writeString(version)
        }

        companion object CREATOR : Parcelable.Creator<LoaderInfo> {
            override fun createFromParcel(parcel: Parcel): LoaderInfo {
                return LoaderInfo(parcel)
            }

            override fun newArray(size: Int): Array<LoaderInfo?> {
                return arrayOfNulls(size)
            }
        }
    }

    data class QuickPlay(
        val hasQuickPlaysSupport: Boolean,
        val isQuickPlaySingleplayer: Boolean,
        val isQuickPlayMultiplayer: Boolean
    ): Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readInt().toBoolean(),
            parcel.readInt().toBoolean(),
            parcel.readInt().toBoolean()
        )

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(hasQuickPlaysSupport.getInt())
            dest.writeInt(isQuickPlaySingleplayer.getInt())
            dest.writeInt(isQuickPlayMultiplayer.getInt())
        }

        companion object CREATOR : Parcelable.Creator<QuickPlay> {
            override fun createFromParcel(parcel: Parcel): QuickPlay {
                return QuickPlay(parcel)
            }

            override fun newArray(size: Int): Array<QuickPlay?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(minecraftVersion)
        dest.writeParcelable(quickPlay, flags)
        dest.writeParcelable(loaderInfo, flags)
    }

    companion object CREATOR : Parcelable.Creator<VersionInfo> {
        override fun createFromParcel(parcel: Parcel): VersionInfo {
            return VersionInfo(parcel)
        }

        override fun newArray(size: Int): Array<VersionInfo?> {
            return arrayOfNulls(size)
        }
    }
}
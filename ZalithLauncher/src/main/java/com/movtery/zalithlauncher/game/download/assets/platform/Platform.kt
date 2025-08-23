package com.movtery.zalithlauncher.game.download.assets.platform

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

/**
 * 可用的资源搜索平台
 */
@Serializable
enum class Platform(val displayName: String): Parcelable {
    CURSEFORGE("CurseForge"),
    MODRINTH("Modrinth");

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(name)
    }

    companion object CREATOR : Parcelable.Creator<Platform> {
        override fun createFromParcel(parcel: Parcel): Platform {
            return Platform.valueOf(
                value = parcel.readString()!!
            )
        }

        override fun newArray(size: Int): Array<out Platform?> {
            return arrayOfNulls(size)
        }
    }
}
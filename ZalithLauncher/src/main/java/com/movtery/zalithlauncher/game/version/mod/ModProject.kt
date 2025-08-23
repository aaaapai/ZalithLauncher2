package com.movtery.zalithlauncher.game.version.mod

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.game.download.assets.platform.Platform

/**
 * 展示到模组管理页面的项目信息
 */
class ModProject(
    val id: String,
    val platform: Platform,
    val iconUrl: String? = null,
    val title: String,
    val slug: String
): Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeParcelable(platform, flags)
        dest.writeString(iconUrl)
        dest.writeString(title)
        dest.writeString(slug)
    }

    companion object CREATOR : Parcelable.Creator<ModProject> {
        override fun createFromParcel(parcel: Parcel): ModProject {
            val id = parcel.readString()!!
            val platform = parcel.readParcelable<Platform>(Platform::class.java.classLoader)!!
            val iconUrl = parcel.readString()
            val title = parcel.readString()!!
            val slug = parcel.readString()!!
            return ModProject(
                id = id,
                platform = platform,
                iconUrl = iconUrl,
                title = title,
                slug = slug
            )
        }

        override fun newArray(size: Int): Array<out ModProject?> {
            return arrayOfNulls(size)
        }
    }
}
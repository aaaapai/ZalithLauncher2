package com.movtery.zalithlauncher.game.version.mod

import android.os.Parcel
import android.os.Parcelable
import com.movtery.zalithlauncher.game.download.assets.platform.ModLoaderDisplayLabel

class ModVersionLoaders(
    val loaders: Array<ModLoaderDisplayLabel>
): Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelableArray(loaders, flags)
    }

    companion object CREATOR : Parcelable.Creator<ModVersionLoaders> {
        override fun createFromParcel(parcel: Parcel): ModVersionLoaders {
            val parcelableArray = parcel.readParcelableArray(ModLoaderDisplayLabel::class.java.classLoader)
            return ModVersionLoaders(
                loaders = Array(parcelableArray!!.size) { i ->
                    parcelableArray[i] as ModLoaderDisplayLabel
                }
            )
        }

        override fun newArray(size: Int): Array<out ModVersionLoaders?> {
            return arrayOfNulls(size)
        }
    }
}
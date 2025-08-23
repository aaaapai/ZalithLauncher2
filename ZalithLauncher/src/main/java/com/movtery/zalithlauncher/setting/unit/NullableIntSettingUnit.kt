package com.movtery.zalithlauncher.setting.unit

import com.movtery.zalithlauncher.setting.launcherMMKV

class NullableIntSettingUnit(key: String, defaultValue: Int?) : AbstractSettingUnit<Int?>(key, defaultValue) {
    override fun getValue(): Int? {
        val mmkv = launcherMMKV()
        return if (mmkv.containsKey(key)) {
            mmkv.getInt(key, 0)
        } else {
            defaultValue
        }.also { state = it }
    }

    override fun saveValue(v: Int?) {
        val mmkv = launcherMMKV()
        if (v == null) {
            mmkv.remove(key).apply()
        } else {
            mmkv.putInt(key, v).apply()
        }
    }
}
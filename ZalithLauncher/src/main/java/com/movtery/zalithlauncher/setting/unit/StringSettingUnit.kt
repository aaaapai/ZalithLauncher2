package com.movtery.zalithlauncher.setting.unit

import com.movtery.zalithlauncher.setting.launcherMMKV

class StringSettingUnit(key: String, defaultValue: String) : AbstractSettingUnit<String>(key, defaultValue) {
    override fun getValue(): String {
        return launcherMMKV().getString(key ,defaultValue)!!
            .also { state = it }
    }

    override fun saveValue(v: String) {
        launcherMMKV().putString(key, v).apply()
    }
}
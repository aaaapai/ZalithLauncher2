package com.movtery.zalithlauncher.setting.unit

import com.movtery.zalithlauncher.setting.launcherMMKV

class IntSettingUnit(key: String, defaultValue: Int) : AbstractSettingUnit<Int>(key, defaultValue) {
    override fun getValue(): Int {
        return launcherMMKV().getInt(key, defaultValue)
            .also { state = it }
    }

    override fun saveValue(v: Int) {
        launcherMMKV().putInt(key, v).apply()
    }
}
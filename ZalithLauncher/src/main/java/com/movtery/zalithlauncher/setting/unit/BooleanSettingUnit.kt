package com.movtery.zalithlauncher.setting.unit

import com.movtery.zalithlauncher.setting.launcherMMKV

class BooleanSettingUnit(key: String, defaultValue: Boolean) : AbstractSettingUnit<Boolean>(key, defaultValue) {
    override fun getValue(): Boolean {
        return launcherMMKV().getBoolean(key, defaultValue)
            .also { state = it }
    }

    override fun saveValue(v: Boolean) {
        launcherMMKV().putBoolean(key, v).apply()
    }
}
package com.movtery.zalithlauncher.setting.unit

import com.movtery.zalithlauncher.setting.launcherMMKV

class FloatSettingUnit(key: String, defaultValue: Float) : AbstractSettingUnit<Float>(key, defaultValue) {
    override fun getValue(): Float {
        return launcherMMKV().getFloat(key, defaultValue)
            .also { state = it }
    }

    override fun saveValue(v: Float) {
        launcherMMKV().putFloat(key, v).apply()
    }
}
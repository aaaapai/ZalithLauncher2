package com.movtery.zalithlauncher.setting.unit

import com.movtery.zalithlauncher.setting.Settings.Manager

class FloatSettingUnit(key: String, defaultValue: Float) : AbstractSettingUnit<Float>(key, defaultValue) {
    override fun getValue(): Float {
        if (cacheValue != null) return cacheValue!!
        return Manager.getValue(key, defaultValue) {
            it.toFloatOrNull()
        }.also {
            cacheValue = it
        }
    }
}
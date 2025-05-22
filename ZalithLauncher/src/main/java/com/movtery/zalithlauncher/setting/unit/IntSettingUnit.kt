package com.movtery.zalithlauncher.setting.unit

import com.movtery.zalithlauncher.setting.Settings.Manager

class IntSettingUnit(key: String, defaultValue: Int) : AbstractSettingUnit<Int>(key, defaultValue) {
    override fun getValue(): Int {
        if (cacheValue != null) return cacheValue!!
        return Manager.getValue(key, defaultValue) {
            it.toIntOrNull()
        }.also {
            cacheValue = it
        }
    }
}
package com.movtery.zalithlauncher.setting.unit

import com.movtery.zalithlauncher.setting.Settings.Manager

class StringSettingUnit(key: String, defaultValue: String) : AbstractSettingUnit<String>(key, defaultValue) {
    override fun getValue(): String {
        if (cacheValue != null) return cacheValue!!
        return Manager.getValue(key, defaultValue) { it }.also { cacheValue = it }
    }
}
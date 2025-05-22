package com.movtery.zalithlauncher.setting.unit

import com.movtery.zalithlauncher.setting.Settings.Manager

class LongSettingUnit(key: String, defaultValue: Long) : AbstractSettingUnit<Long>(key, defaultValue) {
    override fun getValue(): Long {
        if (cacheValue != null) return cacheValue!!
        return Manager.getValue(key, defaultValue) {
            it.toLongOrNull()
        }.also {
            cacheValue = it
        }
    }
}
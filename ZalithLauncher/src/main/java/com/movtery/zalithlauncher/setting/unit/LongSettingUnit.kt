package com.movtery.zalithlauncher.setting.unit

import com.movtery.zalithlauncher.setting.launcherMMKV

class LongSettingUnit(key: String, defaultValue: Long) : AbstractSettingUnit<Long>(key, defaultValue) {
    override fun getValue(): Long {
        return launcherMMKV().getLong(key, defaultValue)
            .also { state = it }
    }

    override fun saveValue(v: Long) {
        launcherMMKV().putLong(key, v).apply()
    }
}
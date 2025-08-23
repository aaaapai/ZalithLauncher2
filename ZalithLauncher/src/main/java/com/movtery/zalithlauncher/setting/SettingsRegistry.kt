package com.movtery.zalithlauncher.setting

import com.movtery.zalithlauncher.setting.unit.AbstractSettingUnit
import com.movtery.zalithlauncher.setting.unit.BooleanSettingUnit
import com.movtery.zalithlauncher.setting.unit.FloatSettingUnit
import com.movtery.zalithlauncher.setting.unit.IntSettingUnit
import com.movtery.zalithlauncher.setting.unit.LongSettingUnit
import com.movtery.zalithlauncher.setting.unit.NullableIntSettingUnit
import com.movtery.zalithlauncher.setting.unit.StringSettingUnit
import com.movtery.zalithlauncher.setting.unit.enumSettingUnit

abstract class SettingsRegistry {
    protected val refreshableList = mutableListOf<AbstractSettingUnit<*>>()

    fun reloadAll() = refreshableList.forEach { it.init() }

    protected fun boolSetting(key: String, def: Boolean) =
        BooleanSettingUnit(key, def).also { refreshableList.add(it) }

    protected fun intSetting(key: String, def: Int) =
        IntSettingUnit(key, def).also { refreshableList.add(it) }

    protected fun intSetting(key: String, def: Int?) =
        NullableIntSettingUnit(key, def).also { refreshableList.add(it) }

    protected fun floatSetting(key: String, def: Float) =
        FloatSettingUnit(key, def).also { refreshableList.add(it) }

    protected fun longSetting(key: String, def: Long) =
        LongSettingUnit(key, def).also { refreshableList.add(it) }

    protected fun stringSetting(key: String, def: String) =
        StringSettingUnit(key, def).also { refreshableList.add(it) }

    protected inline fun <reified E : Enum<E>> enumSetting(key: String, def: E) =
        enumSettingUnit(key, def).also { refreshableList.add(it) }
}
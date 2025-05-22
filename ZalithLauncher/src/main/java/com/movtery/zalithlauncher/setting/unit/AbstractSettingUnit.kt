package com.movtery.zalithlauncher.setting.unit

import androidx.annotation.CheckResult
import com.movtery.zalithlauncher.setting.Settings

abstract class AbstractSettingUnit<V>(
    val key: String,
    val defaultValue: V
) {
    protected var cacheValue: V? = null

    /**
     * @return 获取当前的设置值
     */
    abstract fun getValue(): V

    /**
     * @return 存入值，并返回一个设置构建器
     */
    @CheckResult
    fun put(value: V): Settings.Manager.SettingBuilder {
        this.cacheValue = value!!
        return Settings.Manager.SettingBuilder().put(this, value)
    }

    /**
     * 重置当前设置单元为默认值
     */
    fun reset() {
        this.cacheValue = defaultValue!!
        Settings.Manager.put(this, defaultValue).save()
    }
}
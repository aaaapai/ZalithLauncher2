package com.movtery.zalithlauncher.setting.unit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

abstract class AbstractSettingUnit<V>(
    val key: String,
    val defaultValue: V
) {
    /**
     * @return 获取当前的设置值
     */
    abstract fun getValue(): V

    /**
     * 保存设置值
     */
    protected abstract fun saveValue(v: V)

    /**
     * 可观察的状态
     */
    var state by mutableStateOf(defaultValue)
        protected set

    fun init() {
        this.state = getValue()
    }

    /**
     * @return 存入值，并返回一个设置构建器
     */
    fun save(value: V) {
        this.state = value
        return saveValue(value)
    }

    /**
     * @return **仅更新状态**，不保存值
     */
    fun updateState(value: V) {
        this.state = value
    }

    /**
     * 重置当前设置单元为默认值
     */
    fun reset() {
        this.state = defaultValue
        saveValue(defaultValue)
    }
}
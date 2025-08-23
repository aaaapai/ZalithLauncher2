package com.movtery.zalithlauncher.ui.control.input

/**
 * 当前输入模式
 */
enum class TextInputMode {
    /**
     * 启用
     */
    ENABLE {
        override fun switch() = DISABLE
    },

    /**
     * 禁用
     */
    DISABLE {
        override fun switch() = ENABLE
    };

    abstract fun switch(): TextInputMode
}
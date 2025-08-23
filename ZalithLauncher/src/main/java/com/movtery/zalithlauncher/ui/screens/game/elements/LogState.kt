package com.movtery.zalithlauncher.ui.screens.game.elements

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.movtery.zalithlauncher.setting.AllSettings

/**
 * 控制日志的显示状态
 */
enum class LogState(val value: Boolean) {
    /**
     * 仅在游戏开始渲染前显示日志
     */
    SHOW_BEFORE_LOADING(true) {
        override fun next(): LogState = CLOSE
    },

    /**
     * 任何时候都显示日志
     */
    SHOW(true) {
        override fun next(): LogState = CLOSE
    },

    /**
     * 任何时候都不显示日志
     */
    CLOSE(false) {
        override fun next(): LogState = SHOW
    };

    abstract fun next(): LogState

    companion object {
        fun mutableStateOfLog(): MutableState<LogState> {
            return mutableStateOf(
                if (AllSettings.showLogAutomatic.getValue()) SHOW_BEFORE_LOADING
                else CLOSE
            )
        }
    }
}
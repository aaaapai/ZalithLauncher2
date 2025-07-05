package com.movtery.zalithlauncher.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ObjectStates {
    private val _throwableFlow = MutableStateFlow<ThrowableMessage?>(null)

    /**
     * 状态：需要展示的错误信息
     */
    val throwableFlow: StateFlow<ThrowableMessage?> = _throwableFlow

    /**
     * 需要展示的错误信息
     */
    fun updateThrowable(tm: ThrowableMessage?) {
        _throwableFlow.value = tm
    }

    data class ThrowableMessage(val title: String, val message: String)
}
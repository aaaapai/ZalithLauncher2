package com.movtery.zalithlauncher.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class EventViewModel : ViewModel() {
    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    /**
     * 发送一个事件
     */
    fun sendEvent(event: Event) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    sealed interface Event {
        sealed interface Key : Event {
            /** 让MainActivity开始按键捕获 */
            data object StartKeyCapture : Key
            /** 让MainActivity停止按键捕获 */
            data object StopKeyCapture : Key
            /** 由MainActivity发送的按键捕获结果 */
            data class OnKeyDown(val key: KeyEvent) : Key
        }
        sealed interface Game : Event {
            /** 呼出IME */
            data object ShowIme : Event
            /** 刷新游戏画面分辨率 */
            data object RefreshSize : Event
        }
    }
}
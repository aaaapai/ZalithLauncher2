package com.movtery.zalithlauncher.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class ErrorViewModel : ViewModel() {
    private val _errorEvents = MutableSharedFlow<ThrowableMessage>()
    val errorEvents: SharedFlow<ThrowableMessage> = _errorEvents

    fun showError(message: ThrowableMessage) {
        viewModelScope.launch {
            println("Test: get error = $message")
            _errorEvents.emit(message)
        }
    }

    data class ThrowableMessage(val title: String, val message: String)
}
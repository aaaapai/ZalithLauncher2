package com.movtery.zalithlauncher.ui.control.input

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnKeyboardClosed(
    onClosed: () -> Unit
) {
    val imeVisible = WindowInsets.isImeVisible
    var lastImeVisible by remember { mutableStateOf(imeVisible) }

    LaunchedEffect(imeVisible) {
        if (lastImeVisible && !imeVisible) {
            onClosed()
        }
        lastImeVisible = imeVisible
    }
}
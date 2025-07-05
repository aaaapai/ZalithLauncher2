package com.movtery.zalithlauncher.ui.screens.splash.elements

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey

/**
 * 状态：当前启动屏幕的标签
 */
var splashScreenKey by mutableStateOf<NavKey?>(null)
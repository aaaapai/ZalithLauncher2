package com.movtery.zalithlauncher.ui.screens.main.elements

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.ui.screens.clearWith
import com.movtery.zalithlauncher.ui.screens.content.LauncherScreenKey

/**
 * 主屏幕堆栈
 */
val mainScreenBackStack = mutableStateListOf<NavKey>(LauncherScreenKey)

/**
 * 状态：当前主屏幕的标签
 */
var mainScreenKey by mutableStateOf<NavKey?>(null)

/**
 * 返回主页面
 */
fun SnapshotStateList<NavKey>.backToMainScreen() {
    mainScreenBackStack.clearWith(LauncherScreenKey)
}

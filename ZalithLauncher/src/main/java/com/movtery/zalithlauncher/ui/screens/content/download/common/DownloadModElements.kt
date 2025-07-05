package com.movtery.zalithlauncher.ui.screens.content.download.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchModScreenKey

/**
 * 下载模组屏幕堆栈
 */
val downloadModBackStack = mutableStateListOf<NavKey>(SearchModScreenKey)

/**
 * 状态：下载模组子屏幕标签
 */
var downloadModScreenKey by mutableStateOf<NavKey?>(null)
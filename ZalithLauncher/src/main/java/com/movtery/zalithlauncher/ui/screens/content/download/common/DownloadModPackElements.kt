package com.movtery.zalithlauncher.ui.screens.content.download.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchModPackScreenKey

/**
 * 下载整合包屏幕堆栈
 */
val downloadModPackBackStack = mutableStateListOf<NavKey>(SearchModPackScreenKey)

/**
 * 状态：下载整合包子屏幕标签
 */
var downloadModPackScreenKey by mutableStateOf<NavKey?>(null)
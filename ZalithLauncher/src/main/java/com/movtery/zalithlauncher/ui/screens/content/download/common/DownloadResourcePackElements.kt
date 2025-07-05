package com.movtery.zalithlauncher.ui.screens.content.download.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchResourcePackScreenKey

/**
 * 下载资源包屏幕堆栈
 */
val downloadResourcePackBackStack = mutableStateListOf<NavKey>(SearchResourcePackScreenKey)

/**
 * 状态：下载资源包子屏幕标签
 */
var downloadResourcePackScreenKey by mutableStateOf<NavKey?>(null)
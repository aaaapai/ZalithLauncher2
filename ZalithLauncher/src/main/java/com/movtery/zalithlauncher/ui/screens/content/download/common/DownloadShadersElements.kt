package com.movtery.zalithlauncher.ui.screens.content.download.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchShadersScreenKey

/**
 * 下载光影包屏幕堆栈
 */
val downloadShadersBackStack = mutableStateListOf<NavKey>(SearchShadersScreenKey)

/**
 * 状态：下载光影包子屏幕标签
 */
var downloadShadersScreenKey by mutableStateOf<NavKey?>(null)
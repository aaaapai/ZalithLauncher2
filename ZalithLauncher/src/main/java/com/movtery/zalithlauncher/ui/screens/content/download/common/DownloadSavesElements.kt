package com.movtery.zalithlauncher.ui.screens.content.download.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.search.SearchSavesScreenKey

/**
 * 下载存档屏幕堆栈
 */
val downloadSavesBackStack = mutableStateListOf<NavKey>(SearchSavesScreenKey)

/**
 * 状态：下载存档子屏幕标签
 */
var downloadSavesScreenKey by mutableStateOf<NavKey?>(null)
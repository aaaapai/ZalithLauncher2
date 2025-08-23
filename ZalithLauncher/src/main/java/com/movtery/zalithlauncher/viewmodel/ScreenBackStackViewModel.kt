package com.movtery.zalithlauncher.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey

class ScreenBackStackViewModel : ViewModel() {
    /** 主屏幕的屏幕堆栈 */
    val mainScreenBackStack: NavBackStack = mutableStateListOf()
    /** 设置屏幕的屏幕堆栈 */
    val settingsBackStack: NavBackStack = mutableStateListOf()
    /** 版本详细设置屏幕的屏幕堆栈 */
    val versionsBackStack: NavBackStack = mutableStateListOf()
    /** 下载屏幕的屏幕堆栈 */
    val downloadBackStack: NavBackStack = mutableStateListOf()

    //下载嵌套子屏幕
    /** 下载游戏屏幕的屏幕堆栈 */
    val downloadGameBackStack: NavBackStack = mutableStateListOf()
    /** 下载整合包屏幕的屏幕堆栈 */
    val downloadModPackBackStack: NavBackStack = mutableStateListOf()
    /** 下载模组屏幕的屏幕堆栈 */
    val downloadModBackStack: NavBackStack = mutableStateListOf()
    /** 下载资源包屏幕的屏幕堆栈 */
    val downloadResourcePackBackStack: NavBackStack = mutableStateListOf()
    /** 下载存档屏幕的屏幕堆栈 */
    val downloadSavesBackStack: NavBackStack = mutableStateListOf()
    /** 下载光影包屏幕的屏幕堆栈 */
    val downloadShadersBackStack: NavBackStack = mutableStateListOf()

    init {
        mainScreenBackStack.addIfEmpty(NormalNavKey.LauncherMain)
        settingsBackStack.addIfEmpty(NormalNavKey.Settings.Renderer)
        versionsBackStack.addIfEmpty(NormalNavKey.Versions.OverView)
        //下载嵌套子屏幕
        downloadGameBackStack.addIfEmpty(NormalNavKey.DownloadGame.SelectGameVersion)
        downloadModPackBackStack.addIfEmpty(NormalNavKey.SearchModPack)
        downloadModBackStack.addIfEmpty(NormalNavKey.SearchMod)
        downloadResourcePackBackStack.addIfEmpty(NormalNavKey.SearchResourcePack)
        downloadSavesBackStack.addIfEmpty(NormalNavKey.SearchSaves)
        downloadShadersBackStack.addIfEmpty(NormalNavKey.SearchShaders)
    }

    /** 主屏幕的标签 */
    var mainScreenKey by mutableStateOf<NavKey?>(null)
    /** 设置屏幕的标签 */
    var settingsScreenKey by mutableStateOf<NavKey?>(null)
    /** 下载屏幕的标签 */
    var downloadScreenKey by mutableStateOf<NavKey?>(null)

    //下载嵌套子屏幕
    /** 下载游戏屏幕的标签 */
    var downloadGameScreenKey by mutableStateOf<NavKey?>(null)
    /** 下载整合包屏幕的标签 */
    var downloadModPackScreenKey by mutableStateOf<NavKey?>(null)
    /** 下载模组屏幕的标签 */
    var downloadModScreenKey by mutableStateOf<NavKey?>(null)
    /** 下载资源包屏幕的标签 */
    var downloadResourcePackScreenKey by mutableStateOf<NavKey?>(null)
    /** 下载存档屏幕的标签 */
    var downloadSavesScreenKey by mutableStateOf<NavKey?>(null)
    /** 下载光影包屏幕的标签 */
    var downloadShadersScreenKey by mutableStateOf<NavKey?>(null)

    private fun NavBackStack.addIfEmpty(navKey: NavKey) {
        if (isEmpty()) {
            add(navKey)
        }
    }
}
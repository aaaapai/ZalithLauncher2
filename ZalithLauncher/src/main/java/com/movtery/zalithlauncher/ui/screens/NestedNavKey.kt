package com.movtery.zalithlauncher.ui.screens

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.game.version.installed.Version
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * 嵌套NavDisplay的屏幕
 */
sealed interface NestedNavKey : NavKey {
    /** 当前屏幕正在使用的堆栈 */
    val backStack: NavBackStack

    /** 设置屏幕 */
    @Serializable data class Settings(@Contextual override val backStack: NavBackStack) : NestedNavKey
    /** 版本详细设置屏幕 */
    @Serializable data class Versions(
        @Contextual override val backStack: NavBackStack,
        @Contextual val version: Version
    ) : NestedNavKey
    /** 下载屏幕 */
    @Serializable data class Download(@Contextual override val backStack: NavBackStack) : NestedNavKey

    //下载嵌套子屏幕
    /** 下载游戏屏幕 */
    @Serializable data class DownloadGame(@Contextual override val backStack: NavBackStack) : NestedNavKey
    /** 下载整合包屏幕 */
    @Serializable data class DownloadModPack(@Contextual override val backStack: NavBackStack) : NestedNavKey
    /** 下载模组屏幕 */
    @Serializable data class DownloadMod(@Contextual override val backStack: NavBackStack) : NestedNavKey
    /** 下载资源包屏幕 */
    @Serializable data class DownloadResourcePack(@Contextual override val backStack: NavBackStack) : NestedNavKey
    /** 下载存档屏幕 */
    @Serializable data class DownloadSaves(@Contextual override val backStack: NavBackStack) : NestedNavKey
    /** 下载光影包屏幕 */
    @Serializable data class DownloadShaders(@Contextual override val backStack: NavBackStack) : NestedNavKey
}
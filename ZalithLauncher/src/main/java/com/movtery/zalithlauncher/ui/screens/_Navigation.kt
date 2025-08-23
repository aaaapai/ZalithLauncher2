package com.movtery.zalithlauncher.ui.screens

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * 兼容嵌套NavDisplay的返回事件处理
 */
fun onBack(currentBackStack: NavBackStack) {
    val key = currentBackStack.lastOrNull()
    when (key) {
        //普通的屏幕，直接退出当前堆栈的上层
        is NormalNavKey -> currentBackStack.removeLastOrNull()
        is NestedNavKey -> {
            if (key.backStack.size <= 1) {
                //嵌套屏幕的堆栈处于最后一个屏幕的状态
                //可以退出当前堆栈的上层了
                currentBackStack.removeLastOrNull()
            } else {
                //退出子堆栈的上层屏幕
                key.backStack.removeLastOrNull()
            }
        }
    }
}

fun NavBackStack.navigateOnce(key: NavKey) {
    if (key == lastOrNull()) return //防止反复加载
    clearWith(key)
}

fun NavBackStack.navigateTo(key: NavKey) {
    if (key == lastOrNull()) return //防止反复加载
    add(key)
}

fun NavBackStack.navigateTo(screenKey: NavKey, useClassEquality: Boolean = false) {
    if (useClassEquality) {
        val current = lastOrNull()
        if (current != null && screenKey::class == current::class) return //防止反复加载
        add(screenKey)
    } else {
        navigateTo(screenKey)
    }
}

/**
 * 清除所有栈，并加入指定的key
 */
fun NavBackStack.clearWith(navKey: NavKey) {
    //批量替换内容，避免 Nav3 看到空帧
    this.apply {
        clear()
        add(navKey)
    }
}
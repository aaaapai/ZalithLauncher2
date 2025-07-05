package com.movtery.zalithlauncher.ui.screens

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

interface NestedNavKey: NavKey {
    fun isLastScreen(): Boolean
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
 * 清除所有栈，并假如指定的key
 */
fun NavBackStack.clearWith(navKey: NavKey) {
    clear()
    add(navKey)
}
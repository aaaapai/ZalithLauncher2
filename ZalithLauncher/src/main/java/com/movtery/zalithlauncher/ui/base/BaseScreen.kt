package com.movtery.zalithlauncher.ui.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.navigation3.runtime.NavKey

/**
 * 单层级基础屏幕，根据 `currentKey` 判断当前屏幕是否可见
 * @param screenKey 当前屏幕的Key
 * @param currentKey 当前屏幕正在展示的Key
 * @param useClassEquality 是否使用类相等判断
 */
@Composable
fun BaseScreen(
    screenKey: NavKey,
    currentKey: NavKey?,
    useClassEquality: Boolean = false,
    content: @Composable (isVisible: Boolean) -> Unit,
) {
    val targetVisible = remember(currentKey, screenKey, useClassEquality) {
        isTagVisible(screenKey, currentKey, useClassEquality)
    }

    //初始不可见，用于触发首次的 false -> true 动画
    val visibleState = remember { mutableStateOf(false) }

    //仅在 composition 完成后，才允许更新可见状态
    LaunchedEffect(targetVisible) {
        visibleState.value = targetVisible
    }

    BaseScreen(
        content = content,
        visible = visibleState.value
    )
}

/**
 * 多层级基础屏幕，根据层级列表中的每个层级Key判断当前屏幕是否可见
 * @param levels 层级列表，每个层级包含（Key、当前Key、是否启用引用相等）
 */
@Composable
fun BaseScreen(
    vararg levels: Triple<NavKey, NavKey?, Boolean>,
    content: @Composable (isVisible: Boolean) -> Unit,
) {
    val targetVisible = remember(levels) {
        levels.all { (tag, currentKey, useReferenceEquality) ->
            isTagVisible(tag, currentKey, useReferenceEquality)
        }
    }

    //初始不可见，用于触发首次的 false -> true 动画
    val visibleState = remember { mutableStateOf(false) }

    //仅在 composition 完成后，才允许更新可见状态
    LaunchedEffect(targetVisible) {
        visibleState.value = targetVisible
    }

    BaseScreen(
        content = content,
        visible = visibleState.value
    )
}

/**
 * 多层级基础屏幕，根据层级列表中的每个层级Key判断当前屏幕是否可见
 */
@Composable
fun BaseScreen(
    levels1: List<Pair<Class<out NavKey>, NavKey?>>,
    vararg levels2: Triple<NavKey, NavKey?, Boolean>,
    content: @Composable (isVisible: Boolean) -> Unit,
) {
    val targetVisible = remember(levels1, levels2) {
        val v1 = levels1.all { (key, currentKey) ->
            isTagVisible(key, currentKey)
        }
        val v2 = levels2.all { (key, currentKey, useClassEquality) ->
            isTagVisible(key, currentKey, useClassEquality)
        }
        v1 && v2
    }

    //初始不可见，用于触发首次的 false -> true 动画
    val visibleState = remember { mutableStateOf(false) }

    //仅在 composition 完成后，才允许更新可见状态
    LaunchedEffect(targetVisible) {
        visibleState.value = targetVisible
    }

    BaseScreen(
        content = content,
        visible = visibleState.value
    )
}

@Composable
private fun BaseScreen(
    content: @Composable (isVisible: Boolean) -> Unit,
    visible: Boolean
) {
    Box {
        content(visible)

        if (!visible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0f)
                    .clickable { }
            )
        }
    }
}

private fun isTagVisible(key: Class<out NavKey>, current: NavKey?): Boolean {
    return key.isInstance(current)
}

/**
 * @param useClassEquality 是否使用类相等判断
 */
private fun isTagVisible(key: NavKey, current: NavKey?, useClassEquality: Boolean): Boolean {
    return when {
        current == null -> false
        useClassEquality -> key::class == current::class
        else -> key == current
    }
}
package com.movtery.zalithlauncher.ui.control.mouse

import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isForwardPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import com.movtery.zalithlauncher.setting.enums.MouseControlMode
import com.movtery.zalithlauncher.ui.components.FocusableBox
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 原始触摸控制模拟层
 * @param controlMode               控制模式：SLIDE（滑动控制）、CLICK（点击控制）
 * @param longPressTimeoutMillis    长按触发检测时长
 * @param requestPointerCapture     是否使用鼠标抓取方案
 * @param onTap                     点击回调，参数是触摸点在控件内的绝对坐标
 * @param onLongPress               长按开始回调
 * @param onLongPressEnd            长按结束回调
 * @param onPointerMove             指针移动回调，参数在 SLIDE 模式下是指针位置，CLICK 模式下是手指当前位置
 * @param onMouseMove               实体鼠标指针移动回调
 * @param onMouseScroll             实体鼠标指针滚轮滑动
 * @param onMouseButton             实体鼠标指针按钮按下反馈
 * @param inputChange               重新启动内部的 pointerInput 块，让触摸逻辑能够实时拿到最新的外部参数
 */
@Composable
fun TouchpadLayout(
    modifier: Modifier = Modifier,
    controlMode: MouseControlMode = MouseControlMode.SLIDE,
    longPressTimeoutMillis: Long = -1L,
    requestPointerCapture: Boolean = true,
    onTap: (Offset) -> Unit = {},
    onLongPress: () -> Unit = {},
    onLongPressEnd: () -> Unit = {},
    onPointerMove: (Offset) -> Unit = {},
    onMouseMove: (Offset) -> Unit = {},
    onMouseScroll: (Offset) -> Unit = {},
    onMouseButton: (button: Int, pressed: Boolean) -> Unit = { _, _ -> },
    inputChange: Array<out Any> = arrayOf(Unit),
    requestFocusKey: Any? = null
) {
    val viewConfig = LocalViewConfiguration.current
    val interactionSource = remember { MutableInteractionSource() }

    //确保 pointerInput 中总是调用到最新的回调，避免闭包捕获旧值
    val currentControlMode by rememberUpdatedState(controlMode)
    val currentLongPressTimeoutMillis by rememberUpdatedState(longPressTimeoutMillis)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnLongPressEnd by rememberUpdatedState(onLongPressEnd)
    val currentOnPointerMove by rememberUpdatedState(onPointerMove)

    FocusableBox(
        modifier = modifier
            .hoverable(interactionSource)
            .pointerInput(*inputChange) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        if (down.type != PointerType.Touch) {
                            //过滤掉不是触摸的类型
                            return@awaitEachGesture
                        }

                        var pointer = down
                        var isDragging = false
                        var longPressTriggered = false
                        val startPosition = down.position
                        val longPressJob = if (currentControlMode == MouseControlMode.SLIDE) launch {
                            //只在滑动点击模式下进行长按计时
                            val timeout = if (currentLongPressTimeoutMillis > 0) {
                                currentLongPressTimeoutMillis
                            } else {
                                viewConfig.longPressTimeoutMillis
                            }
                            delay(timeout)
                            if (!isDragging) {
                                longPressTriggered = true
                                currentOnLongPress()
                            }
                        } else null

                        if (currentControlMode == MouseControlMode.CLICK) {
                            //点击模式下，如果触摸，无论如何都应该更新指针位置
                            currentOnPointerMove(pointer.position)
                        }

                        drag(down.id) { change ->
                            isDragging = true
                            val delta = change.positionChange()
                            val distanceFromStart = (change.position - startPosition).getDistance()

                            if (currentControlMode == MouseControlMode.SLIDE) {
                                if (distanceFromStart > viewConfig.touchSlop) {
                                    //超出了滑动检测距离，说明是真的在进行滑动
                                    longPressJob?.cancel() //取消长按计时
                                }

                                if (isDragging || longPressTriggered) {
                                    pointer = change
                                    currentOnPointerMove(delta)
                                }
                            } else {
                                if (!longPressTriggered) {
                                    longPressTriggered = true
                                    currentOnLongPress()
                                }
                                pointer = change
                                currentOnPointerMove(change.position)
                            }

                            change.consume()
                        }

                        longPressJob?.cancel()
                        if (longPressTriggered) {
                            currentOnLongPressEnd()
                        } else {
                            when (currentControlMode) {
                                MouseControlMode.SLIDE -> {
                                    if (!isDragging && !longPressTriggered) {
                                        currentOnTap(pointer.position)
                                    }
                                }
                                MouseControlMode.CLICK -> {
                                    //未进入长按，算一次点击事件
                                    currentOnTap(pointer.position)
                                }
                            }
                        }
                    }
                }
            }
            .then(
                Modifier.mouseEventModifier(
                    requestPointerCapture = requestPointerCapture,
                    inputChange = inputChange,
                    onMouseMove = onMouseMove,
                    onMouseScroll = onMouseScroll,
                    onMouseButton = onMouseButton
                )
            ),
        requestKey = requestFocusKey
    )

    SimpleMouseCapture(
        requestPointerCapture = requestPointerCapture,
        onMouseMove = onMouseMove,
        onMouseScroll = onMouseScroll,
        onMouseButton = onMouseButton
    )
}

@Composable
fun SimpleMouseCapture(
    requestPointerCapture: Boolean,
    onMouseMove: (Offset) -> Unit,
    onMouseScroll: (Offset) -> Unit,
    onMouseButton: (button: Int, pressed: Boolean) -> Unit
) {
    val view = LocalView.current
    val currentOnMouseMove by rememberUpdatedState(onMouseMove)
    val currentOnMouseScroll by rememberUpdatedState(onMouseScroll)
    val currentOnMouseButton by rememberUpdatedState(onMouseButton)

    DisposableEffect(view, requestPointerCapture) {
        view.setOnCapturedPointerListener(null)

        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (requestPointerCapture && hasFocus) {
                view.requestPointerCapture()
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)

        if (requestPointerCapture) {
            view.requestFocus()
            if (view.hasWindowFocus()) {
                view.requestPointerCapture()
            }

            val pointerListener = View.OnCapturedPointerListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_HOVER_MOVE, MotionEvent.ACTION_MOVE -> {
                        val relX = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X)
                        val relY = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y)
                        val dx = if (relX != 0f) relX else event.x
                        val dy = if (relY != 0f) relY else event.y
                        currentOnMouseMove(Offset(dx, dy))
                        true
                    }
                    MotionEvent.ACTION_SCROLL -> {
                        currentOnMouseScroll(
                            Offset(
                                event.getAxisValue(MotionEvent.AXIS_HSCROLL),
                                event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                            )
                        )
                        true
                    }
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_BUTTON_PRESS -> {
                        currentOnMouseButton(event.actionButton, true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_BUTTON_RELEASE -> {
                        currentOnMouseButton(event.actionButton, false)
                        true
                    }
                    else -> false
                }
            }

            view.setOnCapturedPointerListener(pointerListener)
        } else {
            view.releasePointerCapture()
            view.setOnCapturedPointerListener(null)
        }

        onDispose {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
            view.setOnCapturedPointerListener(null)
        }
    }
}

private fun Modifier.mouseEventModifier(
    requestPointerCapture: Boolean,
    inputChange: Array<out Any> = arrayOf(Unit),
    onMouseMove: (Offset) -> Unit,
    onMouseScroll: (Offset) -> Unit,
    onMouseButton: (Int, Boolean) -> Unit
) = this.pointerInput(*inputChange, requestPointerCapture) {
    val previousButtonStates = mutableMapOf<Int, Boolean>()

    if (requestPointerCapture) return@pointerInput

    awaitEachGesture {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull()

            val pointerType = change?.type
            if (pointerType != PointerType.Mouse) {
                //过滤掉不是实体鼠标的类型
                continue
            }

            if (event.type == PointerEventType.Move) {
                onMouseMove(change.position)
            }

            //滚动，但是方向要进行取反
            if (event.type == PointerEventType.Scroll) {
                onMouseScroll(-change.scrollDelta)
            }

            detectButtonChanges(previousButtonStates, event, onMouseButton)

            event.changes.forEach { it.consume() }
        }
    }
}

private fun detectButtonChanges(
    previousButtonStates: MutableMap<Int, Boolean>,
    event: PointerEvent,
    onMouseButton: (Int, Boolean) -> Unit
) {
    val buttons = event.buttons

    val buttonStates = mapOf(
        MotionEvent.BUTTON_PRIMARY to buttons.isPrimaryPressed,
        MotionEvent.BUTTON_SECONDARY to buttons.isSecondaryPressed,
        MotionEvent.BUTTON_TERTIARY to buttons.isTertiaryPressed,
        MotionEvent.BUTTON_BACK to buttons.isBackPressed,
        MotionEvent.BUTTON_FORWARD to buttons.isForwardPressed
    )

    for ((button, isPressed) in buttonStates) {
        val previousPressed = previousButtonStates[button] ?: false
        if (previousPressed != isPressed) {
            onMouseButton(button, isPressed)
        }
    }

    previousButtonStates.clear()
    previousButtonStates.putAll(buttonStates)
}
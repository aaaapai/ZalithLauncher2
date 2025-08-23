package com.movtery.zalithlauncher.ui.control.mouse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.bridge.CURSOR_DISABLED
import com.movtery.zalithlauncher.bridge.CURSOR_ENABLED
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MouseControlMode
import com.movtery.zalithlauncher.utils.device.PhysicalMouseChecker

/**
 * 鼠标指针抓取模式
 */
typealias CursorMode = Int

/**
 * 可根据指针抓取模式，自切换的虚拟指针模拟层
 * @param cursorMode                当前指针抓取模式
 * @param controlMode               控制模式：SLIDE（滑动控制）、CLICK（点击控制）
 * @param longPressTimeoutMillis    长按触发检测时长
 * @param requestPointerCapture     是否使用鼠标抓取方案
 * @param hideMouseInClickMode      是否在鼠标为点击控制模式时，隐藏鼠标指针
 * @param onMouseTap                点击回调
 * @param onCapturedTap             抓取模式点击回调，参数是触摸点在控件内的绝对坐标
 * @param onLongPress               长按开始回调
 * @param onLongPressEnd            长按结束回调
 * @param onCapturedLongPress       抓取模式长按开始回调
 * @param onCapturedLongPressEnd    抓取模式长按结束回调
 * @param onPointerMove             指针移动回调，参数在 SLIDE 模式下是指针位置，CLICK 模式下是手指当前位置
 * @param onCapturedMove            抓取模式指针移动回调，返回滑动偏移量
 * @param onMouseScroll             实体鼠标指针滚轮滑动
 * @param onMouseButton             实体鼠标指针按钮按下反馈
 * @param mouseSize                 指针大小
 * @param cursorSensitivity         指针灵敏度（滑动模式生效）
 */
@Composable
fun SwitchableMouseLayout(
    modifier: Modifier = Modifier,
    cursorMode: CursorMode,
    controlMode: MouseControlMode = AllSettings.mouseControlMode.state,
    longPressTimeoutMillis: Long = AllSettings.mouseLongPressDelay.state.toLong(),
    requestPointerCapture: Boolean = !AllSettings.physicalMouseMode.state,
    hideMouseInClickMode: Boolean = AllSettings.hideMouse.state,
    onMouseTap: (Offset) -> Unit = {},
    onCapturedTap: (Offset) -> Unit = {},
    onLongPress: () -> Unit = {},
    onLongPressEnd: () -> Unit = {},
    onCapturedLongPress: () -> Unit = {},
    onCapturedLongPressEnd: () -> Unit = {},
    onPointerMove: (Offset) -> Unit = {},
    onCapturedMove: (Offset) -> Unit = {},
    onMouseScroll: (Offset) -> Unit = {},
    onMouseButton: (button: Int, pressed: Boolean) -> Unit = { _, _ -> },
    mouseSize: Dp = AllSettings.mouseSize.state.dp,
    cursorSensitivity: Int = AllSettings.cursorSensitivity.state
) {
    val windowSize = LocalWindowInfo.current.containerSize
    val screenWidth: Float = windowSize.width.toFloat()
    val screenHeight: Float = windowSize.height.toFloat()
    val centerPos = Offset(screenWidth / 2f, screenHeight / 2f)

    val speedFactor = cursorSensitivity / 100f

    val lastVirtualMousePos = remember { object { var value: Offset? = null } }

    //判断鼠标是否正在被抓取
    val isCaptured by remember(cursorMode) {
        mutableStateOf(
            value = cursorMode == CURSOR_DISABLED
        )
    }

    val isPhysicalMouseShowed = remember(isCaptured) {
        if (PhysicalMouseChecker.physicalMouseConnected) { //物理鼠标已连接
            !requestPointerCapture //根据是否是抓取模式（虚拟鼠标控制模式）判断物理鼠标是否显示
        } else {
            false
        }
    }

    var showMousePointer by remember {
        mutableStateOf(requestPointerCapture)
    }
    fun updateMousePointer(show: Boolean) {
        showMousePointer = show
    }
    LaunchedEffect(cursorMode, hideMouseInClickMode) {
        updateMousePointer(
            show = if (cursorMode == CURSOR_ENABLED) {
                when {
                    //物理鼠标已连接：是否为抓获控制模式
                    PhysicalMouseChecker.physicalMouseConnected -> requestPointerCapture
                    //点击控制模式：由隐藏虚拟鼠标设置决定
                    controlMode == MouseControlMode.CLICK -> !hideMouseInClickMode
                    //滑动控制始终显示
                    else -> controlMode == MouseControlMode.SLIDE
                }
            } else false
        )
    }

    val requestPointerCapture1 by remember(isCaptured) {
        mutableStateOf(
            value = if (isCaptured) true //被抓取时，开启实体鼠标指针抓取模式
            else requestPointerCapture
        )
    }

    fun updatePointerPos(pos: Offset) {
        lastVirtualMousePos.value = pos
        onPointerMove(pos)
    }
    var pointerPosition by remember {
        mutableStateOf(centerPos)
    }
    LaunchedEffect(isCaptured) {
        val pos = lastVirtualMousePos.value?.takeIf {
            //如果当前正在使用物理鼠标，则使用上次虚拟鼠标的位置
            //否则默认将鼠标放到屏幕正中心
            isPhysicalMouseShowed
        } ?: centerPos
        if (!isCaptured) updatePointerPos(pos)
        pointerPosition = pos
    }

    Box(modifier = modifier) {
        if (showMousePointer) {
            MousePointer(
                modifier = Modifier.offset(
                    x = with(LocalDensity.current) { pointerPosition.x.toDp() },
                    y = with(LocalDensity.current) { pointerPosition.y.toDp() }
                ),
                mouseSize = mouseSize,
                mouseFile = getMousePointerFileAvailable()
            )
        }

        TouchpadLayout(
            modifier = Modifier.fillMaxSize(),
            controlMode = if (cursorMode == CURSOR_ENABLED) {
                controlMode
            } else {
                //捕获模式下，只有滑动控制模式才能获取到滑动偏移量
                MouseControlMode.SLIDE
            },
            longPressTimeoutMillis = longPressTimeoutMillis,
            requestPointerCapture = requestPointerCapture1,
            onTap = { fingerPos ->
                when (cursorMode) {
                    CURSOR_DISABLED -> {
                        onCapturedTap(fingerPos)
                    }
                    CURSOR_ENABLED -> {
                        onMouseTap(
                            if (controlMode == MouseControlMode.CLICK) {
                                updateMousePointer(!isCaptured && !hideMouseInClickMode)
                                //当前手指的绝对坐标
                                pointerPosition = fingerPos
                                fingerPos
                            } else {
                                pointerPosition
                            }
                        )
                    }
                }
            },
            onLongPress = {
                when (cursorMode) {
                    CURSOR_DISABLED -> {
                        onCapturedLongPress()
                    }
                    CURSOR_ENABLED -> {
                        onLongPress()
                    }
                }
            },
            onLongPressEnd = {
                when (cursorMode) {
                    CURSOR_DISABLED -> {
                        onCapturedLongPressEnd()
                    }
                    CURSOR_ENABLED -> {
                        onLongPressEnd()
                    }
                }
            },
            onPointerMove = { offset ->
                when (cursorMode) {
                    CURSOR_DISABLED -> {
                        updateMousePointer(false)
                        onCapturedMove(offset)
                    }
                    CURSOR_ENABLED -> {
                        pointerPosition = if (controlMode == MouseControlMode.SLIDE) {
                            updateMousePointer(true)
                            Offset(
                                x = (pointerPosition.x + offset.x * speedFactor).coerceIn(0f, screenWidth),
                                y = (pointerPosition.y + offset.y * speedFactor).coerceIn(0f, screenHeight)
                            )
                        } else {
                            updateMousePointer(!hideMouseInClickMode)
                            //当前手指的绝对坐标
                            offset
                        }
                        updatePointerPos(pointerPosition)
                    }
                }
            },
            onMouseMove = { offset ->
                when (cursorMode) {
                    CURSOR_DISABLED -> {
                        updateMousePointer(false)
                        onCapturedMove(offset)
                    }
                    CURSOR_ENABLED -> {
                        if (requestPointerCapture) {
                            updateMousePointer(true)
                            pointerPosition = Offset(
                                x = (pointerPosition.x + offset.x * speedFactor).coerceIn(0f, screenWidth),
                                y = (pointerPosition.y + offset.y * speedFactor).coerceIn(0f, screenHeight)
                            )
                            updatePointerPos(pointerPosition)
                        } else {
                            //非鼠标抓取模式
                            updateMousePointer(false)
                            pointerPosition = offset
                            updatePointerPos(pointerPosition)
                        }
                    }
                }
            },
            onMouseScroll = onMouseScroll,
            onMouseButton = onMouseButton,
            requestFocusKey = cursorMode
        )
    }
}
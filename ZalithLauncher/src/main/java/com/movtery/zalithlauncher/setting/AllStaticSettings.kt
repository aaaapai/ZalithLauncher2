package com.movtery.zalithlauncher.setting

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.setting.enums.toGestureButtonType
import com.movtery.zalithlauncher.setting.enums.toMouseControlMode

/**
 * 缩放因子
 */
var scaleFactor by mutableFloatStateOf(AllSettings.resolutionRatio.getValue() / 100f)

/**
 * 实体鼠标控制
 */
var physicalMouseMode by mutableStateOf(AllSettings.physicalMouseMode.getValue())

/**
 * 鼠标大小
 */
var mouseSize by mutableIntStateOf(AllSettings.mouseSize.getValue())

/**
 * 虚拟鼠标灵敏度
 */
var cursorSensitivity by mutableIntStateOf(AllSettings.cursorSensitivity.getValue())

/**
 * 被抓获指针移动灵敏度
 */
var mouseCaptureSensitivity by mutableIntStateOf(AllSettings.mouseCaptureSensitivity.getValue())

/**
 * 鼠标控制模式
 */
var mouseControlMode by mutableStateOf(AllSettings.mouseControlMode.toMouseControlMode())

/**
 * 鼠标长按触发延迟
 */
var mouseLongPressDelay by mutableIntStateOf(AllSettings.mouseLongPressDelay.getValue())

/**
 * 手势控制
 */
var gestureControl by mutableStateOf(AllSettings.gestureControl.getValue())

/**
 * 手势控制点击时触发的鼠标按钮
 */
var gestureTapMouseAction by mutableStateOf(AllSettings.gestureTapMouseAction.getValue().toGestureButtonType())

/**
 * 手势控制长按时触发的鼠标按钮
 */
var gestureLongPressMouseAction by mutableStateOf(AllSettings.gestureLongPressMouseAction.getValue().toGestureButtonType())

/**
 * 手势控制长按触发延迟
 */
var gestureLongPressDelay by mutableIntStateOf(AllSettings.gestureLongPressDelay.getValue())

/**
 * GUI 缩放
 */
var mcOptionsGuiScale by mutableIntStateOf(0)
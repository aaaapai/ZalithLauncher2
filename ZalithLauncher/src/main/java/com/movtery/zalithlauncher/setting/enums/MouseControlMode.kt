package com.movtery.zalithlauncher.setting.enums

import com.movtery.zalithlauncher.R

/**
 * 鼠标的两种控制模式：
 * SLIDE = “滑动控制”
 * CLICK = “点击控制”
 */
enum class MouseControlMode(val nameRes: Int) {
    SLIDE(R.string.settings_control_mouse_control_mode_slide),
    CLICK(R.string.settings_control_mouse_control_mode_click)
}
package com.movtery.zalithlauncher.game.input

import android.view.MotionEvent
import com.movtery.zalithlauncher.bridge.ZLBridge

object AWTCharSender : CharacterSenderStrategy {
    override fun sendChar(character: Char) {
        ZLBridge.sendChar(character)
    }

    override fun sendBackspace() {
        ZLBridge.sendKey(' ', AWTInputEvent.VK_BACK_SPACE)
    }

    override fun sendEnter() {
        ZLBridge.sendKey(' ', AWTInputEvent.VK_ENTER)
    }

    /**
     * 获取 AWT 鼠标点击事件
     */
    fun getMouseButton(button: Int): Int? {
        return when (button) {
            MotionEvent.BUTTON_PRIMARY -> AWTInputEvent.BUTTON1_DOWN_MASK
            MotionEvent.BUTTON_SECONDARY -> AWTInputEvent.BUTTON3_DOWN_MASK
            MotionEvent.BUTTON_TERTIARY -> AWTInputEvent.BUTTON2_DOWN_MASK
            else -> null
        }
    }
}
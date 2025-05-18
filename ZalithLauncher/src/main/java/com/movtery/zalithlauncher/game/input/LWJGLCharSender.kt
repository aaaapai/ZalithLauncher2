package com.movtery.zalithlauncher.game.input

import android.view.MotionEvent
import com.movtery.zalithlauncher.game.keycodes.LwjglGlfwKeycode
import org.lwjgl.glfw.CallbackBridge

object LWJGLCharSender : CharacterSenderStrategy {
    override fun sendBackspace() {
        CallbackBridge.sendKeycode(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE.toInt(), '\u0008', 0, 0, true)
        CallbackBridge.sendKeycode(LwjglGlfwKeycode.GLFW_KEY_BACKSPACE.toInt(), '\u0008', 0, 0, false)
    }

    override fun sendEnter() {
        CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ENTER.toInt())
    }

    override fun sendChar(character: Char) {
        CallbackBridge.sendChar(character, 0)
    }

    /**
     * 获取 LWJGL 鼠标点击事件
     */
    fun getMouseButton(button: Int): Short? {
        return when (button) {
            MotionEvent.BUTTON_PRIMARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT
            MotionEvent.BUTTON_SECONDARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_RIGHT
            MotionEvent.BUTTON_TERTIARY -> LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_MIDDLE
            else -> null
        }
    }
}
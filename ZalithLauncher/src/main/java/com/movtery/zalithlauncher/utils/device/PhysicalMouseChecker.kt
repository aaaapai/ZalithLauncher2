package com.movtery.zalithlauncher.utils.device

import android.app.Activity
import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo

object PhysicalMouseChecker {
    /**
     * 当前是否有实体鼠标连接
     */
    var physicalMouseConnected = false
        private set

    fun initChecker(activity: Activity) {
        //粗检测，因为启动软件前可能已经连接实体鼠标了
        physicalMouseConnected = isPhysicalMouseConnected()
        lInfo("Initialization complete, physical mouse connection status: $physicalMouseConnected")

        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                if (deviceId.isMouseId()) {
                    lInfo("Physical mouse connected, deviceId: $deviceId")
                    physicalMouseConnected = true
                }
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                if (deviceId.isMouseId()) {
                    lInfo("Physical mouse disconnected, deviceId: $deviceId")
                    physicalMouseConnected = false
                } else {
                    physicalMouseConnected = isPhysicalMouseConnected()
                    lInfo("Fallback check for physical mouse connection status: $physicalMouseConnected")
                }
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                //ignore
            }
        }

        val inputManager = activity.getSystemService(Context.INPUT_SERVICE) as InputManager
        inputManager.registerInputDeviceListener(listener, null)
    }
}

/**
 * 确认这个deviceId是否为实体鼠标
 */
private fun Int.isMouseId(): Boolean {
    return InputDevice.getDevice(this)?.let { device ->
        device.sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE
    } ?: false
}

/**
 * 粗略检查是否接入实体鼠标
 */
private fun isPhysicalMouseConnected(): Boolean {
    return InputDevice.getDeviceIds()
        .takeIf { it.isNotEmpty() }
        ?.any { it.isMouseId() }
        ?: false
}
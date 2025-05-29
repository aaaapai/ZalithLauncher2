package com.movtery.zalithlauncher.game.support.touch_controller

import android.os.VibrationEffect
import android.os.Vibrator
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import top.fifthlight.touchcontroller.proxy.client.LauncherProxyClient
import top.fifthlight.touchcontroller.proxy.message.VibrateMessage

class VibrationHandler(
    private val vibrator: Vibrator,
    private val vibrateDuration: Int?,
) : LauncherProxyClient.VibrationHandler {
    override fun viberate(kind: VibrateMessage.Kind) {
        runCatching {
            val duration = vibrateDuration?.coerceAtMost(500)?.coerceAtLeast(80)?.toLong()
            val effect = VibrationEffect.createOneShot(
                duration ?: 100L,
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            vibrator.vibrate(effect)
        }.onFailure {
            lError("Failed to attempt vibrating the device!", it)
        }
    }
}
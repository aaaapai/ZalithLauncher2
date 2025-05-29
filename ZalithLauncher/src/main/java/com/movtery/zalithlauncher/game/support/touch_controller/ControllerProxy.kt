package com.movtery.zalithlauncher.game.support.touch_controller

import android.content.Context
import android.os.Vibrator
import android.system.Os
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import top.fifthlight.touchcontroller.proxy.client.LauncherProxyClient
import top.fifthlight.touchcontroller.proxy.client.android.transport.UnixSocketTransport

/**
 * 为适配 TouchController 模组
 * [Touch Controller](https://modrinth.com/mod/touchcontroller)
 */
object ControllerProxy {
    var proxyClient: LauncherProxyClient? = null
        private set

    /**
     * 启动控制代理客户端，目的是与 TouchController 模组进行通信
     */
    fun startProxy(context: Context, vibrateDuration: Int?) {
        if (proxyClient == null) {
            try {
                val transport = UnixSocketTransport(InfoDistributor.LAUNCHER_NAME)
                Os.setenv("TOUCH_CONTROLLER_PROXY_SOCKET", InfoDistributor.LAUNCHER_NAME, true)
                val client = LauncherProxyClient(transport)
                val vibrator = context.getSystemService(Vibrator::class.java)
                val handler = VibrationHandler(vibrator, vibrateDuration)
                client.vibrationHandler = handler
                client.run()
                LoggerBridge.append("TouchController: TouchController Proxy Client has been created!")
                proxyClient = client
            } catch (ex: Throwable) {
                lWarning("TouchController proxy client create failed", ex)
                proxyClient = null
            }
        }
    }
}
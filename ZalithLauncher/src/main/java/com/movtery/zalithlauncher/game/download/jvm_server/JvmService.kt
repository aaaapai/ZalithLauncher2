package com.movtery.zalithlauncher.game.download.jvm_server

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.compose.ui.unit.IntSize
import androidx.core.app.NotificationCompat
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.bridge.LoggerBridge
import com.movtery.zalithlauncher.game.launch.JvmLaunchInfo
import com.movtery.zalithlauncher.game.launch.JvmLauncher
import com.movtery.zalithlauncher.game.launch.Launcher
import com.movtery.zalithlauncher.notification.NotificationChannelData
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

class JvmService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val notificationId: Int = 1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        postNotification()

        val jvmArgs = intent?.extras?.getString(SERVICE_JVM_ARGS) ?: error("The JVM parameters must be set.")
        val jreName = intent.extras?.getString(SERVICE_JRE_NAME)
        val userHome = intent.extras?.getString(SERVICE_USER_HOME)

        scope.launch(Dispatchers.Default) {
            preLaunch (
                jvmArgs = jvmArgs,
                jreName = jreName,
                userHome = userHome,
                onExit = { code, _ ->
                    lInfo("Process exit with code $code")
                    scope.launch(Dispatchers.IO) {
                        sendCode(code)
                        stopSelf()
                    }
                }
            )
        }

        return START_NOT_STICKY
    }

    private fun sendCode(code: Int) {
        try {
            DatagramSocket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", PROCESS_SERVICE_PORT))
                val data = (code.toString() + "").toByteArray()
                val packet = DatagramPacket(data, data.size)
                socket.send(packet)
                lInfo("Send code $code to 127.0.0.1:$PROCESS_SERVICE_PORT")
            }
        } catch (e: Exception) {
            lError("Failed to send exit code", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    private fun postNotification() {
        //Jvm服务渠道
        val data = NotificationChannelData.JVM_SERVICE_CHANNEL

        val notification: Notification = NotificationCompat.Builder(this, data.channelId)
            .setContentTitle(getString(R.string.notification_data_jvm_service_running))
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(notificationId, notification)
    }

    private suspend fun preLaunch(
        jvmArgs: String,
        jreName: String? = null,
        userHome: String? = null,
        onExit: (code: Int, isSignal: Boolean) -> Unit
    ) {
        val jvmLaunchInfo = JvmLaunchInfo(
            jvmArgs = jvmArgs,
            jreName = jreName,
            userHome = userHome
        )

        val launcher = JvmLauncher(
            context = applicationContext,
            getWindowSize = {
                IntSize(1920, 1080) //fake
            },
            jvmLaunchInfo = jvmLaunchInfo,
            onExit = onExit
        )

        runJvm(launcher, onExit)
    }

    private suspend fun runJvm(
        launcher: Launcher,
        onExit: (code: Int, isSignal: Boolean) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            //在主线程加载 exec
            System.loadLibrary("pojavexec")
        }

        //开始记录日志
        val logFile = File(PathManager.DIR_FILES_EXTERNAL, "latest_process.log")
        if (!logFile.exists() && !logFile.createNewFile()) throw IOException("Failed to create a new log file")
        LoggerBridge.start(logFile.absolutePath)

        lInfo("start jvm!")

        val code = runCatching {
            launcher.launch()
        }.onFailure { e ->
            lWarning("jvm crashed!", e)
        }.getOrElse { 1 }

        onExit(code, false)
    }
}
package com.movtery.zalithlauncher.bridge

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import com.movtery.zalithlauncher.context.GlobalContext
import com.movtery.zalithlauncher.game.launch.Launcher
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.utils.file.shareFile
import com.movtery.zalithlauncher.utils.killProgress
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import java.io.File

object ZLNativeInvoker {
    @JvmStatic
    var staticLauncher: Launcher? = null

    @JvmStatic
    fun openLink(link: String) {
        (GlobalContext as? Activity)?.let { activity ->
            activity.runOnUiThread {
                var prefix = "file:"
                if (link.startsWith(prefix)) {
                    if (link.startsWith("file://")) prefix += "//"
                    val newLink = link.removePrefix(prefix)
                    lInfo("open link: $newLink")

                    val file = File(newLink)
                    shareFile(activity, file)
                    lInfo("In-game Share File/Folder: ${file.absolutePath}")
                } else {
                    NetWorkUtils.openLink(activity, link, "*/*")
                }
            }
        }
    }

    @JvmStatic
    fun querySystemClipboard() {
        (GlobalContext as? Activity)?.let { activity ->
            activity.runOnUiThread {
                val clipData = (activity.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.primaryClip ?: run {
                    ZLBridge.clipboardReceived(null, null)
                    return@runOnUiThread
                }
                val clipItemText = clipData.getItemAt(0).text ?: run {
                    ZLBridge.clipboardReceived(null, null)
                    return@runOnUiThread
                }
                ZLBridge.clipboardReceived(clipItemText.toString(), "plain")
            }
        }
    }

    @JvmStatic
    fun putClipboardData(data: String, mimeType: String) {
        (GlobalContext as? Activity)?.let { activity ->
            activity.runOnUiThread {
                val clipData = when (mimeType) {
                    "text/plain" -> ClipData.newPlainText(InfoDistributor.LAUNCHER_IDENTIFIER, data)
                    "text/html" -> ClipData.newHtmlText(InfoDistributor.LAUNCHER_IDENTIFIER, data, data)
                    else -> null
                }
                clipData?.let {
                    (activity.getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(it)
                }
            }
        }
    }

    @JvmStatic
    fun putFpsValue(fps: Int) {
        ZLBridgeStates.currentFPS = fps
    }

    @JvmStatic
    fun jvmExit(exitCode: Int, isSignal: Boolean) {
        staticLauncher?.onExit?.invoke(exitCode, isSignal)
        staticLauncher = null
        killProgress()
    }
}
package com.movtery.zalithlauncher.components.jre

import android.content.Context
import android.content.res.AssetManager
import com.movtery.zalithlauncher.ZLApplication
import com.movtery.zalithlauncher.components.AbstractUnpackTask
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.file.readString
import com.movtery.zalithlauncher.utils.logging.Logger.lError

class UnpackJreTask(
    private val context: Context,
    private val jre: Jre
) : AbstractUnpackTask() {
    private lateinit var assetManager: AssetManager
    private lateinit var launcherRuntimeVersion: String
    private var isCheckFailed: Boolean = false

    init {
        runCatching {
            assetManager = context.assets
            launcherRuntimeVersion = assetManager.open(jre.jrePath + "/version").readString()
        }.getOrElse {
            isCheckFailed = true
        }
    }

    fun isCheckFailed() = isCheckFailed

    override fun isNeedUnpack(): Boolean {
        if (isCheckFailed) return false

        return runCatching {
            val installedRuntimeVersion = RuntimesManager.loadInternalRuntimeVersion(jre.jreName)
            return launcherRuntimeVersion != installedRuntimeVersion
        }.onFailure { e ->
            lError("An exception occurred while detecting the Java Runtime.", e)
        }.getOrElse { false }
    }

    override suspend fun run() {
        runCatching {
            RuntimesManager.installRuntimeBinPack(
                universalFileInputStream = assetManager.open(jre.jrePath + "/universal.tar.xz"),
                platformBinsInputStream = assetManager.open(
                    jre.jrePath + "/bin-" + Architecture.archAsString(ZLApplication.DEVICE_ARCHITECTURE) + ".tar.xz"
                ),
                name = jre.jreName,
                binPackVersion = launcherRuntimeVersion,
                updateProgress = { textRes, textArgs ->
                    taskMessage = context.getString(textRes, *textArgs)
                }
            )
            RuntimesManager.postPrepare(jre.jreName)
        }.onFailure {
            lError("Internal JRE unpack failed", it)
        }.getOrThrow()
    }
}
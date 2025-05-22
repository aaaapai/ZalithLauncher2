package com.movtery.zalithlauncher.game.plugin.ffmpeg

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.movtery.zalithlauncher.game.plugin.ApkPlugin
import com.movtery.zalithlauncher.game.plugin.cacheAppIcon
import java.io.File

object FFmpegPluginManager {
    private const val PLUGIN_PACKAGE_NAME = "net.kdt.pojavlaunch.ffmpeg"

    var libraryPath: String? = null
        private set

    var executablePath: String? = null
        private set

    /**
     * 插件是否可用
     */
    var isAvailable: Boolean = false
        private set

    /**
     * 加载 FFmpeg 插件
     */
    fun loadPlugin(
        context: Context,
        loaded: (ApkPlugin) -> Unit = {}
    ) {
        val manager: PackageManager = context.packageManager
        runCatching {
            val info = manager.getPackageInfo(
                PLUGIN_PACKAGE_NAME,
                PackageManager.GET_SHARED_LIBRARY_FILES
            )
            val applicationInfo = info.applicationInfo!!
            libraryPath = applicationInfo.nativeLibraryDir
            val ffmpegExecutable = File(libraryPath, "libffmpeg.so")
            executablePath = ffmpegExecutable.absolutePath
            isAvailable = ffmpegExecutable.exists()

            if (isAvailable) {
                cacheAppIcon(context, applicationInfo)
                runCatching {
                    ApkPlugin(
                        packageName = PLUGIN_PACKAGE_NAME,
                        appName = applicationInfo.loadLabel(manager).toString(),
                        appVersion = manager.getPackageInfo(PLUGIN_PACKAGE_NAME, 0).versionName ?: ""
                    )
                }.getOrNull()?.let { loaded(it) }
            }
        }.onFailure { e ->
            Log.w("FFmpegPluginManager", "Failed to discover plugin", e)
        }
    }
}
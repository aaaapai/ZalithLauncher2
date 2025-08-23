package com.movtery.zalithlauncher.game.plugin

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.plugin.ffmpeg.FFmpegPluginManager
import com.movtery.zalithlauncher.game.plugin.renderer.RendererPluginManager
import com.movtery.zalithlauncher.game.renderer.RendererInterface.Companion.toInterface
import com.movtery.zalithlauncher.game.renderer.Renderers

/**
 * 统一插件的加载，保证仅获取一次应用列表
 */
object PluginLoader {
    private var isInitialized: Boolean = false
    private const val PACKAGE_FLAGS =
        PackageManager.GET_META_DATA or PackageManager.GET_SHARED_LIBRARY_FILES

    /**
     * 所有已加载的应用插件
     */
    var allPlugins: List<ApkPlugin> = emptyList()
        private set

    @JvmStatic
    @SuppressLint("QueryPermissionsNeeded")
    fun loadAllPlugins(context: Context, force: Boolean = false) {
        if (isInitialized && !force) return
        isInitialized = true

        val apkPluginList: MutableList<ApkPlugin> = mutableListOf()

        DriverPluginManager.initDriver(context, force)
        if (force) RendererPluginManager.clearPlugin()

        val queryIntentActivities =
            context.packageManager.queryIntentActivities(
                Intent("android.intent.action.MAIN"),
                PACKAGE_FLAGS
            )
        queryIntentActivities.forEach { resolve ->
            val applicationInfo = resolve.activityInfo.applicationInfo
            DriverPluginManager.parsePlugin(context, applicationInfo) { apkPluginList.add(it) }
            RendererPluginManager.parseApkPlugin(context, applicationInfo) { apkPluginList.add(it) }
        }
        FFmpegPluginManager.loadPlugin(context) { apkPluginList.add(it) }

        if (RendererPluginManager.isAvailable()) {
            RendererPluginManager.getRendererList().filter { plugin ->
                !Renderers.addRenderer(plugin.toInterface())
            }.takeIf {
                it.isNotEmpty()
            }?.let { failedToLoadList ->
                RendererPluginManager.removeRenderer(failedToLoadList)
            }
        }

        //全部已加载的插件
        allPlugins = apkPluginList.sortedBy { it.appName }
    }
}
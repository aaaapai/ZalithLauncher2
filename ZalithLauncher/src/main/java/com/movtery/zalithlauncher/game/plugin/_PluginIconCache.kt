package com.movtery.zalithlauncher.game.plugin

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.util.Log
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.image.toBitmap
import org.apache.commons.io.FileUtils
import java.io.File

const val PLUGIN_ICON_CACHE_TAG = "PluginIconCache"

fun appCacheIcon(packageName: String): File = File(PathManager.DIR_CACHE_APP_ICON, "$packageName.png")

/**
 * 缓存应用的图标到本地缓存目录，便于通过包名临时加载
 */
fun cacheAppIcon(context: Context, appInfo: ApplicationInfo) {
    val packageName = appInfo.packageName
    val iconFile = appCacheIcon(packageName)

    if (iconFile.exists()) return

    runCatching {
        context.packageManager.let { manager ->
            //读取图标，转换为bitmap
            val icon = appInfo.loadIcon(manager).toBitmap()
            //开始缓存
            iconFile.outputStream().use { stream ->
                icon.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }.onFailure {
        FileUtils.deleteQuietly(iconFile)
        Log.w(PLUGIN_ICON_CACHE_TAG, "Failed to cache icon for $packageName at ${iconFile.absolutePath}", it)
    }
}
package com.movtery.zalithlauncher

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Process
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.movtery.zalithlauncher.context.getContextWrapper
import com.movtery.zalithlauncher.context.refreshContext
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.path.GamePathManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.loadAllSettings
import com.movtery.zalithlauncher.ui.activities.showFatalError
import com.movtery.zalithlauncher.ui.activities.showLauncherCrash
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.logging.Logger
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.writeCrashFile
import com.tencent.mmkv.MMKV
import okio.Path.Companion.toOkioPath
import kotlin.properties.Delegates

class ZLApplication : Application(), SingletonImageLoader.Factory {
    companion object {
        @JvmStatic
        var DEVICE_ARCHITECTURE by Delegates.notNull<Int>()
    }

    override fun onCreate() {
        Thread.setDefaultUncaughtExceptionHandler { _, th ->
            //停止所有任务
            TaskSystem.stopAll()

            val throwable = if (th is SplashException) th.cause!!
            else th

            lError("An exception occurred", throwable)

            writeCrashFile(
                file = PathManager.FILE_CRASH_REPORT,
                throwable = throwable
            ) { t ->
                lError("An exception occurred while saving the crash report", t)
            }

            showLauncherCrash(this@ZLApplication, throwable, th !is SplashException)
            Process.killProcess(Process.myPid())
        }

        super.onCreate()
        runCatching {
            MMKV.initialize(this)
            loadAllSettings(this)

            Logger.initialize(this)

            initializeData()
            PathManager.DIR_FILES_PRIVATE = getDir("files", MODE_PRIVATE)
            DEVICE_ARCHITECTURE = Architecture.getDeviceArchitecture()
            //Force x86 lib directory for Asus x86 based zenfones
            if (Architecture.isx86Device() && Architecture.is32BitsDevice) {
                val originalJNIDirectory = applicationInfo.nativeLibraryDir
                applicationInfo.nativeLibraryDir = originalJNIDirectory.substring(
                    0,
                    originalJNIDirectory.lastIndexOf("/")
                ) + "/x86"
            }
        }.onFailure { launchTh ->
            writeCrashFile(
                file = PathManager.FILE_CRASH_REPORT,
                throwable = launchTh
            ) {
                Log.w("ZLApplication", "An exception occurred while saving the crash report", it)
            }
            showFatalError(this, launchTh)
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(getContextWrapper(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshContext(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(20L * 1024 * 1024) // 20MB
                    .weakReferencesEnabled(true) //弱引用
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .maxSizeBytes(512L * 1024 * 1024) // 512MB
                    .directory(PathManager.DIR_IMAGE_CACHE.toOkioPath())
                    .build()
            }
            .components { add(GifDecoder.Factory()) }
            .crossfade(true)
            .build()
    }

    private fun initializeData() {
        AccountsManager.initialize(this)
        GamePathManager.initialize(this)
    }
}
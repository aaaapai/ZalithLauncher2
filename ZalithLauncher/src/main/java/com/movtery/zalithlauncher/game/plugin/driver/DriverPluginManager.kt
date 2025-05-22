package com.movtery.zalithlauncher.game.plugin.driver

import android.content.Context
import android.content.pm.ApplicationInfo
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.plugin.ApkPlugin
import com.movtery.zalithlauncher.game.plugin.cacheAppIcon
import com.movtery.zalithlauncher.setting.AllSettings

/**
 * FCL 驱动器插件
 * [FCL DriverPlugin.kt](https://github.com/FCL-Team/FoldCraftLauncher/blob/main/FCLauncher/src/main/java/com/tungsten/fclauncher/plugins/DriverPlugin.kt)
 */
object DriverPluginManager {
    private val driverList: MutableList<Driver> = mutableListOf()

    @JvmStatic
    fun getDriverList(): List<Driver> = driverList.toList()

    private lateinit var currentDriver: Driver

    @JvmStatic
    fun setDriverById(driverId: String) {
        currentDriver = driverList.find { it.id == driverId } ?: driverList[0]
    }

    @JvmStatic
    fun getDriver(): Driver = currentDriver

    /**
     * 初始化驱动器
     * @param reset 是否清除已有插件
     */
    fun initDriver(context: Context, reset: Boolean) {
        if (reset) driverList.clear()
        val applicationInfo = context.applicationInfo
        driverList.add(
            Driver(
                id = AllSettings.vulkanDriver.defaultValue,
                name = "Turnip",
                path = applicationInfo.nativeLibraryDir
            )
        )
        setDriverById(AllSettings.vulkanDriver.getValue())
    }

    /**
     * 通用 FCL 插件
     */
    fun parsePlugin(
        context: Context,
        info: ApplicationInfo,
        loaded: (ApkPlugin) -> Unit = {}
    ) {
        if (info.flags and ApplicationInfo.FLAG_SYSTEM == 0) {
            val metaData = info.metaData ?: return
            if (metaData.getBoolean("fclPlugin", false)) {
                val driver = metaData.getString("driver") ?: return
                val nativeLibraryDir = info.nativeLibraryDir

                val packageManager = context.packageManager
                val packageName = info.packageName
                val appName = info.loadLabel(packageManager).toString()

                driverList.add(
                    Driver(
                        id = packageName,
                        name = driver,
                        summary = context.getString(R.string.settings_renderer_from_plugins, appName),
                        path = nativeLibraryDir
                    )
                )

                runCatching {
                    cacheAppIcon(context, info)
                    ApkPlugin(
                        packageName = packageName,
                        appName = appName,
                        appVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: ""
                    )
                }.getOrNull()?.let { loaded(it) }
            }
        }
    }
}
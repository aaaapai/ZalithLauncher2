package com.movtery.zalithlauncher.setting

import android.content.Context
import com.movtery.zalithlauncher.game.launch.Launcher
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.platform.MemoryUtils
import com.movtery.zalithlauncher.utils.platform.bytesToMB

private const val LWJGL_LIB_NAME_ARG = "-Dorg.lwjgl.opengl.libname="

/**
 * 初始化处理所有设置项
 * @param reloadAll 是否重新加载全部设置项
 */
fun loadAllSettings(context: Context, reloadAll: Boolean = false) {
    if (reloadAll) AllSettings.reloadAll()
    if (AllSettings.ramAllocation.getValue() == -1) {
        val ram = findBestRAMAllocation(context)
        AllSettings.ramAllocation.save(ram)
    }
    val jvmArgs = AllSettings.jvmArgs.getValue()
    Launcher.parseJavaArguments(jvmArgs).find { it.startsWith(LWJGL_LIB_NAME_ARG) }?.let { arg ->
        AllSettings.jvmArgs.save(jvmArgs.replace(arg, ""))
    }
}

/**
 * This functions aims at finding the best default RAM amount,
 * according to the RAM amount of the physical device.
 * Put not enough RAM ? Minecraft will lag and crash.
 * Put too much RAM ?
 * The GC will lag, android won't be able to breathe properly.
 * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/5de6822/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/prefs/LauncherPreferences.java#L142-L154)
 * @param context Context needed to get the total memory of the device.
 * @return The best default value found.
 */
fun findBestRAMAllocation(context: Context): Int {
    if (Architecture.is32BitsDevice) return 696

    val deviceRam = MemoryUtils.getTotalMemory(context).bytesToMB()
    return when {
        deviceRam < 1024 -> 296
        deviceRam < 1536 -> 448
        deviceRam < 2048 -> 656
        deviceRam < 3064 -> 936
        deviceRam < 4096 -> 1144
        deviceRam < 6144 -> 1536
        else -> 2048 //Default RAM allocation for 64 bits
    }
}
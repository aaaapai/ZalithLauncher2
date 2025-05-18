package com.movtery.zalithlauncher.utils.device

import android.content.pm.PackageManager

fun checkVulkanSupport(packageManager: PackageManager): Boolean {
    return packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL) &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
}
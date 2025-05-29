package com.movtery.zalithlauncher.utils.device

import android.os.Build

/**
 * [from Architecture.java](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/Architecture.java)
 */
object Architecture {
    const val UNSUPPORTED_ARCH = -1
    const val ARCH_ARM64 = 0x1
    const val ARCH_ARM = 0x2
    const val ARCH_X86 = 0x4
    const val ARCH_X86_64 = 0x8

    const val ADDRESS_SPACE_LIMIT_32_BIT: Long = 0xbfffffffL
    const val ADDRESS_SPACE_LIMIT_64_BIT: Long = 0x7fffffffffL

    fun getAddressSpaceLimit() = if (is64BitsDevice) ADDRESS_SPACE_LIMIT_64_BIT else ADDRESS_SPACE_LIMIT_32_BIT

    val is64BitsDevice: Boolean
        get() = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty()

    val is32BitsDevice: Boolean
        get() = !is64BitsDevice

    fun getDeviceArchitecture(): Int {
        return if (isx86Device()) {
            if (is64BitsDevice) ARCH_X86_64 else ARCH_X86
        } else {
            if (is64BitsDevice) ARCH_ARM64 else ARCH_ARM
        }
    }

    fun isx86Device(): Boolean {
        val ABIs = if (is64BitsDevice) Build.SUPPORTED_64_BIT_ABIS else Build.SUPPORTED_32_BIT_ABIS
        val comparedArch = if (is64BitsDevice) ARCH_X86_64 else ARCH_X86
        return ABIs.any { archAsInt(it) == comparedArch }
    }

    fun archAsInt(arch: String?): Int {
        val normalizedArch = arch?.lowercase()?.trim()?.replace(" ", "") ?: return UNSUPPORTED_ARCH
        return when {
            normalizedArch.contains("arm64") || normalizedArch == "aarch64" -> ARCH_ARM64
            normalizedArch.contains("arm") || normalizedArch == "aarch32" -> ARCH_ARM
            normalizedArch.contains("x86_64") || normalizedArch.contains("amd64") -> ARCH_X86_64
            normalizedArch.contains("x86") || (normalizedArch.startsWith("i") && normalizedArch.endsWith("86")) -> ARCH_X86
            else -> UNSUPPORTED_ARCH
        }
    }

    fun archAsString(arch: Int): String = when (arch) {
        ARCH_ARM64 -> "arm64"
        ARCH_ARM -> "arm"
        ARCH_X86_64 -> "x86_64"
        ARCH_X86 -> "x86"
        else -> "UNSUPPORTED_ARCH"
    }
}

package com.movtery.zalithlauncher.game.version.mod.reader

import org.apache.commons.compress.archivers.zip.ZipFile
import java.util.zip.ZipFile as JDKZipFile

fun JDKZipFile.tryGetIcon(iconPath: String?): ByteArray? {
    if (iconPath.isNullOrBlank()) return null

    return try {
        getInputStream(getEntry(iconPath))?.readBytes()
    } catch (_: Exception) {
        null //忽略图标读取错误
    }
}

fun ZipFile.tryGetIcon(iconPath: String?): ByteArray? {
    if (iconPath.isNullOrBlank()) return null

    return try {
        getEntry(iconPath)?.let { entry ->
            getInputStream(entry).use { it.readBytes() }
        }
    } catch (_: Exception) {
        null //忽略图标读取错误
    }
}
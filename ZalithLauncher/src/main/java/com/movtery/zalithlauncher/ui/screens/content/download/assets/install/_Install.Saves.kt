package com.movtery.zalithlauncher.ui.screens.content.download.assets.install

import com.movtery.zalithlauncher.utils.file.extractFromZip
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

/**
 * 解压存档压缩包
 */
suspend fun unpackSaveZip(zipFile: File, targetPath: File) {
    val path = extractLevelPath(zipFile) ?: throw IOException("Unable to locate the level where the level.dat file is stored.")
    lInfo("Found the level of the level.data file: $path")
    ZipFile(zipFile).use { zip ->
        zip.extractFromZip(path, File(targetPath, zipFile.nameWithoutExtension))
        lInfo("Decompression is complete")
    }
    FileUtils.deleteQuietly(zipFile)
}

/**
 * 读取zip文件，并找到level.data文件所在的路径
 * @param file 压缩包文件
 */
private fun extractLevelPath(file: File): String? {
    if (!file.exists() || !file.isFile) {
        return null
    }

    if (!file.name.endsWith(".zip", ignoreCase = true)) {
        return null
    }

    ZipFile(file).use { zip ->
        val entries = zip.entries().asSequence() //转换为序列，方便过滤
        val levelDatEntry = entries.find { it.name.endsWith("level.dat", ignoreCase = true) }
        if (levelDatEntry == null) {
            return null
        }
        val path = levelDatEntry.name
        val levelPath = path.substringBeforeLast("/")
        return levelPath
    }
}
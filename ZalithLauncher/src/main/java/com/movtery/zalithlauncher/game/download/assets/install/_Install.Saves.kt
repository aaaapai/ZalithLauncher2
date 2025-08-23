package com.movtery.zalithlauncher.game.download.assets.install

import com.movtery.zalithlauncher.utils.file.CompressZipEntryAdapter
import com.movtery.zalithlauncher.utils.file.JavaZipEntryAdapter
import com.movtery.zalithlauncher.utils.file.UnpackZipException
import com.movtery.zalithlauncher.utils.file.ZipEntryBase
import com.movtery.zalithlauncher.utils.file.extractFromZip
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile as JDKZipFile

/**
 * 解压存档压缩包
 */
suspend fun unpackSaveZip(zipFile: File, targetPath: File) {
    val path = extractLevelPath(zipFile) ?: throw IOException("Unable to locate the level where the level.dat file is stored.")
    lInfo("Found the level of the level.data file: $path")
    val target = File(targetPath, zipFile.nameWithoutExtension)
    try {
        JDKZipFile(zipFile).use { zip ->
            zip.extractFromZip(path, target)
            lInfo("Decompression is complete")
        }
    } catch (e: Exception) {
        if (e !is UnpackZipException) {
            tryApacheZip(zipFile, path, target)
        } else {
            throw e
        }
    }
    FileUtils.deleteQuietly(zipFile)
}

private suspend fun tryApacheZip(zipFile: File, path: String, target: File) {
    FileUtils.deleteQuietly(target) //清除一次目标文件夹（如果之前解压出错）
    val zipFile1 = ZipFile.Builder()
        .setFile(zipFile)
        .get()

    zipFile1.use { zip ->
        zip.extractFromZip(path, target)
        lInfo("Decompression is complete")
    }
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

    try {
        JDKZipFile(file).use { zip ->
            val entries = zip.entries().asSequence()
                .map { entry ->
                    JavaZipEntryAdapter(entry)
                }
            return findLevelEntryName(entries)
        }
    } catch (e: Exception) {
        if (e !is UnpackZipException) {
            return extractLevelPathWithApacheZip(file)
        } else {
            throw e
        }
    }
}

private fun extractLevelPathWithApacheZip(file: File): String? {
    val zipFile = ZipFile.Builder()
        .setFile(file)
        .get()

    zipFile.use { zip ->
        val entries = zip.entries.asSequence()
            .map { entry ->
                CompressZipEntryAdapter(entry)
            }
        return findLevelEntryName(entries)
    }
}

private fun <T> findLevelEntryName(
    entries: Sequence<T>
): String? where T : ZipEntryBase {
    val levelDatEntry = entries.find { it.name.endsWith("level.dat", ignoreCase = true) }
    if (levelDatEntry == null) {
        return null
    }
    val path = levelDatEntry.name
    val levelPath = path.substringBeforeLast("/")
    return levelPath
}
package com.movtery.zalithlauncher.game.download.game

import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movtery.zalithlauncher.game.download.game.models.LibraryComponents
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.utils.file.ensureDirectory
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import java.io.File
import java.io.IOException

fun GameManifest.isOldVersion(): Boolean = !minecraftArguments.isNullOrEmpty()

/**
 * 尝试读取指定路径的文件为一个JsonObject对象
 * 若格式不正确则抛出异常
 */
fun String?.getJsonOrNull(tag: String): JsonObject? {
    return this?.let { path ->
        val text: String = File(path).takeIf { it.exists() && it.isFile }?.readText() ?: run {
            Log.w("_DownloadUtils.Game.getJsonOrNull", "The $tag json file is invalid!")
            return@let null
        }
        if (!text.startsWith("{")) {
            Log.w("_DownloadUtils.Game.getJsonOrNull", "The $tag JSON is invalid, first part of the content: ${text.take(1000)}")
            return@let null
        }
        text.parseToJson()
    }
}

/**
 * 快速解析为JsonObject
 */
fun String.parseToJson(): JsonObject = JsonParser.parseString(this).asJsonObject

fun copyLibraries(
    from: File,
    to: File,
    onProgress: ((Float) -> Unit)? = null
) {
    val normalizedFrom = from.absoluteFile.normalize()
    val normalizedTo = to.absoluteFile.normalize()

    val allFiles = mutableListOf<File>()

    normalizedFrom.walkTopDown().forEach { file ->
        val targetPath = File(normalizedTo, file.relativeTo(normalizedFrom).path)
        if (file.isDirectory) {
            targetPath.mkdirs()
        } else {
            allFiles.add(file)
        }
    }

    val fileCount = allFiles.size

    if (fileCount == 0) {
        onProgress?.invoke(1.0f)
        return
    }

    allFiles.forEachIndexed { index, file ->
        val targetFile = File(normalizedTo, file.relativeTo(normalizedFrom).path)
        try {
            targetFile.ensureParentDirectory()
            file.copyTo(targetFile, overwrite = true)
            Log.i("CopyLibraries", "copied: ${file.path} -> ${targetFile.path}")
        } catch (e: IOException) {
            Log.e("CopyLibraries", "Failed to copy: ${file.path} -> ${targetFile.path}", e)
        }
        onProgress?.invoke((index + 1).toFloat() / fileCount)
    }
}

/**
 * 复制jar、json文件到临时游戏目录，作为安装ModLoader的环境
 * @param sourceGameFolder 源游戏目录
 * @param sourceVersion 源游戏版本名
 * @param destinationGameFolder 要复制到的游戏目录
 * @param targetVersion 要复制为的版本名称
 * @param filesToCopy 指定要复制的文件的后缀名
 */
fun copyVanillaFiles(
    sourceGameFolder: File,
    sourceVersion: String,
    destinationGameFolder: File,
    targetVersion: String,
    filesToCopy: List<String> = listOf(".json", ".jar")
) {
    val sourceDir = File(sourceGameFolder, "versions/$sourceVersion").ensureDirectory()
    val destinationDir = File(destinationGameFolder, "versions/$targetVersion").ensureDirectory()

    for (extension in filesToCopy) {
        val sourceFile = File(sourceDir, "$sourceVersion$extension")
        val destinationFile = File(destinationDir, "$targetVersion$extension")

        if (!destinationFile.exists() && sourceFile.exists()) {
            sourceFile.copyTo(destinationFile)
        }
    }
}

/**
 * 根据提供的原始库名称生成对应的本地路径。
 * @param original 库的原始名称，例如 `groupId:artifactId:version`
 * @param baseFolder 基础文件夹路径，作为文件路径前缀
 */
fun getLibraryPath(
    original: String,
    baseFolder: String
): String {
    val components = parseLibraryComponents(original)

    // 处理 OptiFine 特殊情况
    if (isOptiFineLibrary(components.groupId, components.artifactId, components.version)) {
        val specialPath = handleOptiFineSpecialCase(
            baseFolder = baseFolder,
            groupId = components.groupId,
            artifactId = components.artifactId,
            version = components.version
        )
        if (specialPath != null) return specialPath
    }

    val groupIdPath = components.groupId.replace(".", File.separator)
    val classifierSuffix = if (!components.classifier.isNullOrEmpty()) "-${components.classifier}" else ""
    val jarName = "${components.artifactId}-${components.version}$classifierSuffix.jar"

    return listOf(
        "$baseFolder/libraries",
        groupIdPath,
        components.artifactId,
        components.version,
        jarName
    ).joinToString(File.separator)
}

/**
 * 解析原始库名称字符串为组件（groupId、artifactId、version）
 */
fun parseLibraryComponents(original: String): LibraryComponents {
    val components = original.split(":")
    require(components.size >= 3) { "Invalid library name: $original" }
    return LibraryComponents(
        groupId = components[0],
        artifactId = components[1],
        version = components[2],
        classifier = components.getOrNull(3)
    )
}

private fun buildArtifactPath(
    baseFolder: String?,
    groupId: String,
    artifactId: String,
    version: String,
    jarName: String
): String = buildString {
    baseFolder?.let { append(it).append(File.separator).append("libraries").append(File.separator) }
    append(listOf(groupId, artifactId, version).joinToString(File.separator))
    append(File.separator).append(jarName)
}

private fun isOptiFineLibrary(
    groupId: String,
    artifactId: String,
    version: String
) = groupId == "optifine" && artifactId == "OptiFine" && version.startsWith("1.")

private fun handleOptiFineSpecialCase(
    baseFolder: String?,
    groupId: String,
    artifactId: String,
    version: String
): String? {
    val (major, minor) = parseOptiFineVersion(version)
    if (!shouldUseInstaller(major, minor)) return null

    val installerJarName = "$artifactId-$version-installer.jar"
    val installerPath = buildArtifactPath(baseFolder, groupId, artifactId, version, installerJarName)

    return installerPath.takeIf { File(it).exists() }
}

private fun parseOptiFineVersion(version: String): Pair<Int, Int> {
    val parts = version.split(".", "_")
    return Pair(
        parts.getOrNull(1)?.toIntOrNull() ?: 0,
        parts.getOrNull(2)?.toIntOrNull() ?: 0
    )
}

private fun shouldUseInstaller(major: Int, minor: Int) =
    major == 12 || (major == 20 && minor >= 4) || major >= 21

package com.movtery.zalithlauncher.game.version.download

import com.movtery.zalithlauncher.game.addons.mirror.mapMirrorableUrls
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.file.compareSHA1
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import com.movtery.zalithlauncher.utils.network.withRetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

private const val UTILS_LOG_TAG = "Minecraft.DownloaderUtils"

fun <T> String.parseTo(classOfT: Class<T>): T {
    return runCatching {
        GSON.fromJson(this, classOfT)
    }.getOrElse { e ->
        lError("Failed to parse JSON", e)
        throw e
    }
}

suspend fun <T> downloadAndParseJson(
    targetFile: File,
    url: String,
    expectedSHA: String?,
    verifyIntegrity: Boolean,
    classOfT: Class<T>
): T {
    suspend fun downloadAndParse(): T {
        val json = withContext(Dispatchers.IO) {
            val string = withRetry(UTILS_LOG_TAG, maxRetries = 1) {
                NetWorkUtils.fetchStringFromUrls(
                    url.mapMirrorableUrls()
                )
            }
            if (string.isBlank()) {
                lError("Downloaded string is empty, aborting.")
                throw IllegalStateException("Downloaded string is empty.")
            }
            targetFile.writeText(string)
            string
        }
        return json.parseTo(classOfT)
    }

    if (targetFile.exists()) {
        if (!verifyIntegrity || compareSHA1(targetFile, expectedSHA)) {
            return runCatching {
                targetFile.readText().parseTo(classOfT)
            }.getOrElse {
                lWarning("Failed to parse existing JSON, re-downloading...")
                downloadAndParse()
            }
        } else {
            FileUtils.deleteQuietly(targetFile)
        }
    }

    return downloadAndParse()
}

fun artifactToPath(library: GameManifest.Library): String? {
    library.downloads?.artifact?.path?.let { return it }

    val libInfos = library.name.split(":")
    if (libInfos.size < 3) {
        lError("Invalid library name format: ${library.name}")
        return null
    }

    val groupId = libInfos[0].replace('.', '/')
    val artifactId = libInfos[1]
    val version = libInfos[2]
    val classifier = if (libInfos.size > 3) "-${libInfos[3]}" else ""

    return "$groupId/$artifactId/$version/$artifactId-$version$classifier.jar"
}

fun processLibraries(libraries: () -> List<GameManifest.Library>) {
    libraries().forEach { library ->
        processLibrary(library)
    }
}

private fun processLibrary(library: GameManifest.Library) {
    val versionSegment = library.name.split(":").getOrNull(2) ?: return
    val versionParts = versionSegment.split(".")

    getLibraryReplacement(library.name, versionParts)?.let { replacement ->
        lDebug("Library ${library.name} has been changed to version ${replacement.newName.split(":").last()}")
        updateLibrary(library, replacement)
    }
}

private fun updateLibrary(
    library: GameManifest.Library,
    replacement: LibraryReplacement
) {
    createLibraryInfo(library)
    library.name = replacement.newName
    library.downloads.artifact.apply {
        path = replacement.newPath
        sha1 = replacement.newSha1
        url = replacement.newUrl
    }
}

private fun createLibraryInfo(library: GameManifest.Library) {
    if (library.downloads?.artifact == null) {
        library.downloads = GameManifest.DownloadsX().apply {
            this.artifact = GameManifest.Artifact()
        }
    }
}

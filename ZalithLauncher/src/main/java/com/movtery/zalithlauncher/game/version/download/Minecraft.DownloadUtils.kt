package com.movtery.zalithlauncher.game.version.download

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

private suspend fun downloadStringAndSave(
    url: String,
    targetFile: File
): String =
    withContext(Dispatchers.IO) {
        val string = withRetry(UTILS_LOG_TAG, maxRetries = 1) {
            NetWorkUtils.fetchStringFromUrl(url)
        }
        if (string.isBlank()) {
            lError("Downloaded string is empty, aborting.")
            throw IllegalStateException("Downloaded string is empty.")
        }
        targetFile.writeText(string)
        string
    }

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
        val json = downloadStringAndSave(url, targetFile)
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

    fun logChange(libraryName: String, newVersion: String) {
        lDebug("Library $libraryName has been changed to version $newVersion")
    }

    when {
        library.name.startsWith("net.java.dev.jna:jna:") ->
            processJnaLibrary(library, versionParts) { name, version -> logChange(name, version) }
        library.name.startsWith("com.github.oshi:oshi-core:") ->
            processOshiLibrary(library, versionParts) { name, version -> logChange(name, version) }
        library.name.startsWith("org.ow2.asm:asm-all:") ->
            processAsmLibrary(library, versionParts) { name, version -> logChange(name, version) }
    }
}

private fun processJnaLibrary(library: GameManifest.Library, versionParts: List<String>, log: (name: String, version: String) -> Unit) {
    val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = versionParts.getOrNull(1)?.toIntOrNull() ?: 0

    //如果版本已经达到5.13.0及以上，则不做处理
    if (major >= 5 && minor >= 13) return

    log(library.name, "5.13.0")
    updateLibrary(
        library,
        newName = "net.java.dev.jna:jna:5.13.0",
        newPath = "net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar",
        newSha1 = "1200e7ebeedbe0d10062093f32925a912020e747",
        newUrl = "https://repo1.maven.org/maven2/net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar"
    )
}

private fun processOshiLibrary(library: GameManifest.Library, versionParts: List<String>, log: (name: String, version: String) -> Unit) {
    val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 0
    val minor = versionParts.getOrNull(1)?.toIntOrNull() ?: 0

    //仅对版本 6.2.0 进行修改
    if (major != 6 || minor != 2) return

    log(library.name, "6.3.0")
    updateLibrary(
        library,
        newName = "com.github.oshi:oshi-core:6.3.0",
        newPath = "com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar",
        newSha1 = "9e98cf55be371cafdb9c70c35d04ec2a8c2b42ac",
        newUrl = "https://repo1.maven.org/maven2/com/github/oshi/oshi-core/6.3.0/oshi-core-6.3.0.jar"
    )
}

private fun processAsmLibrary(library: GameManifest.Library, versionParts: List<String>, log: (name: String, version: String) -> Unit) {
    val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 0

    //如果主版本号不低于5，则不做处理
    if (major >= 5) return

    log(library.name, "5.0.4")
    createLibraryInfo(library)
    library.name = "org.ow2.asm:asm-all:5.0.4"
    library.url = null
    library.downloads.artifact.apply {
        path = "org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
        sha1 = "e6244859997b3d4237a552669279780876228909"
        url = "https://repo1.maven.org/maven2/org/ow2/asm/asm-all/5.0.4/asm-all-5.0.4.jar"
    }
}

private fun updateLibrary(
    library: GameManifest.Library,
    newName: String,
    newPath: String,
    newSha1: String,
    newUrl: String
) {
    createLibraryInfo(library)
    library.name = newName
    library.downloads.artifact.apply {
        path = newPath
        sha1 = newSha1
        url = newUrl
    }
}

private fun createLibraryInfo(library: GameManifest.Library) {
    if (library.downloads?.artifact == null) {
        library.downloads = GameManifest.DownloadsX().apply {
            this.artifact = GameManifest.Artifact()
        }
    }
}

package com.movtery.zalithlauncher.game.multirt

import android.system.Os
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.components.jre.Jre
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.utils.file.child
import com.movtery.zalithlauncher.utils.file.ensureDirectory
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.math.findNearestPositive
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.extractUntilCharacter
import com.movtery.zalithlauncher.utils.string.compareVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/v3_openjdk/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/multirt/MultiRTUtils.java)
 */
object RuntimesManager {
    private val cache = ConcurrentHashMap<String, Runtime>()

    private val RUNTIME_FOLDER = PathManager.DIR_MULTIRT
    private const val JAVA_VERSION_STR: String = "JAVA_VERSION=\""
    private const val OS_ARCH_STR: String = "OS_ARCH=\""

    fun getRuntimes(forceLoad: Boolean = false): List<Runtime> {
        if (!RUNTIME_FOLDER.exists()) {
            lWarning("Runtime directory not found: ${RUNTIME_FOLDER.absolutePath}")
            return emptyList()
        }

        return RUNTIME_FOLDER.listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { loadRuntime(it.name, forceLoad = forceLoad) }
            ?.sortedWith { o1, o2 ->
                val thisVer = o1.versionString ?: o1.name
                -thisVer.compareVersion(o2.versionString ?: o2.name)
            }
            ?: throw IllegalStateException("Failed to access runtime directory")
    }

    fun getExactJreName(majorVersion: Int): String? {
        return getRuntimes().firstOrNull { it.javaVersion == majorVersion }?.name
    }

    fun getNearestJreName(majorVersion: Int): String? {
        return findNearestPositive(majorVersion, getRuntimes()) { it.javaVersion }?.value?.name
    }

    fun forceReload(name: String): Runtime {
        cache.remove(name)
        return loadRuntime(name)
    }

    fun loadRuntime(name: String, forceLoad: Boolean = false): Runtime {
        return cache[name]?.takeIf { !forceLoad } ?: run {
            val runtimeDir = File(RUNTIME_FOLDER, name)
            val releaseFile = File(runtimeDir, "release")

            if (!releaseFile.exists()) return Runtime(name).also { cache[name] = it }

            runCatching {
                val content = releaseFile.readText()
                val javaVersion = content.extractUntilCharacter(JAVA_VERSION_STR, '"')
                val osArch = content.extractUntilCharacter(OS_ARCH_STR, '"')

                if (javaVersion != null && osArch != null) {
                    val versionParts = javaVersion.split('.')
                    val majorVersion = if (versionParts.first() == "1") {
                        versionParts.getOrNull(1)?.toIntOrNull() ?: 0
                    } else {
                        versionParts.first().toIntOrNull() ?: 0
                    }
                    Runtime(name, javaVersion, osArch, majorVersion, Jre.entries.any { it.jreName == name })
                } else {
                    Runtime(name)
                }
            }.onFailure { e ->
                lError("Failed to load runtime $name", e)
            }.getOrElse {
                Runtime(name)
            }.also { cache[name] = it }
        }
    }

    @Throws(IOException::class)
    suspend fun installRuntime(
        nativeLibDir: String,
        inputStream: InputStream,
        name: String,
        updateProgress: (Int, Array<Any>) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val dest = RUNTIME_FOLDER.child(name)
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        uncompressTarXZ(inputStream, dest, updateProgress)
        unpack200(nativeLibDir, dest.absolutePath)
        loadRuntime(name)
    }

    @Throws(IOException::class)
    suspend fun postPrepare(name: String) = withContext(Dispatchers.IO) {
        val dest = RUNTIME_FOLDER.child(name)
        if (!dest.exists()) return@withContext
        val runtime = loadRuntime(name)
        var libFolder = "lib"
        if (File(dest, "$libFolder${File.separator}${runtime.arch}").exists()) {
            libFolder += "${File.separator}${runtime.arch}"
        }
        val ftIn = dest.child(libFolder, "libfreetype.so.6")
        val ftOut = dest.child(libFolder, "libfreetype.so")
        if (ftIn.exists() && (!ftOut.exists() || ftIn.length() != ftOut.length())) {
            if (!ftIn.renameTo(ftOut)) throw IOException("Failed to rename freetype")
        }

        //Refresh libraries
        copyDummyNativeLib("libawt_xawt.so", dest, libFolder)
    }

    @Throws(IOException::class)
    suspend fun installRuntimeBinPack(
        universalFileInputStream: InputStream,
        platformBinsInputStream: InputStream,
        name: String,
        binPackVersion: String,
        updateProgress: (Int, Array<Any>) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        val dest = RUNTIME_FOLDER.child(name)
        if (dest.exists()) FileUtils.deleteDirectory(dest)
        installRuntimeNoRemove(universalFileInputStream, dest, updateProgress)
        installRuntimeNoRemove(platformBinsInputStream, dest, updateProgress)

        unpack200(PathManager.DIR_NATIVE_LIB, dest.absolutePath)

        val versionFile = File(dest, "version")
        versionFile.writeText(binPackVersion)

        forceReload(name)
    }

    fun loadInternalRuntimeVersion(name: String): String? {
        val versionFile = RUNTIME_FOLDER.child(name, "version")
        try {
            return if (versionFile.exists()) {
                versionFile.readText()
            } else {
                null
            }
        } catch (_: IOException) {
            return null
        }
    }

    @Throws(IOException::class)
    fun removeRuntime(name: String) {
        val dest: File = RUNTIME_FOLDER.child(name).takeIf { it.exists() } ?: return
        FileUtils.deleteDirectory(dest)
        cache.remove(name)
    }

    fun getRuntimeHome(name: String): File {
        val dest = RUNTIME_FOLDER.child(name)
        if (!dest.exists() || forceReload(name).versionString == null) {
            throw RuntimeException("Selected runtime is broken!")
        }
        return dest
    }

    /**
     * Unpacks all .pack files into .jar Serves only for java 8, as java 9 brought project jigsaw
     * @param nativeLibraryDir The native lib path, required to execute the unpack200 binary
     * @param runtimePath The path to the runtime to walk into
     */
    private suspend fun unpack200(
        nativeLibraryDir: String,
        runtimePath: String
    ) = withContext(Dispatchers.Default) {
            val basePath = File(runtimePath)
            val files: Collection<File> = FileUtils.listFiles(basePath, arrayOf("pack"), true)

            val workDir = File(nativeLibraryDir)
            val processBuilder = ProcessBuilder().directory(workDir)

            files.forEach { jarFile ->
                ensureActive()
                runCatching {
                    val destPath = jarFile.absolutePath.replace(".pack", "")
                    processBuilder.command(
                        "./libunpack200.so",
                        "-r",
                        jarFile.absolutePath,
                        destPath
                    ).start().apply {
                        waitFor()
                    }
                }.onFailure { e ->
                    if (e is IOException) {
                        lError("Failed to unpack the runtime!", e)
                    } else throw e
                }
            }
        }

    @Throws(IOException::class)
    private suspend fun copyDummyNativeLib(
        name: String,
        dest: File,
        libFolder: String
    ) = withContext(Dispatchers.IO) {
        val fileLib = dest.child(libFolder, name)
        val inputStream = FileInputStream(File(PathManager.DIR_NATIVE_LIB, name))
        val outputStream = FileOutputStream(fileLib)
        IOUtils.copy(inputStream, outputStream)
        inputStream.close()
        outputStream.close()
    }

    @Throws(IOException::class)
    private suspend fun installRuntimeNoRemove(
        inputStream: InputStream,
        dest: File,
        updateProgress: (Int, Array<Any>) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        uncompressTarXZ(inputStream, dest, updateProgress)
        inputStream.close()
    }

    @Throws(IOException::class)
    private suspend fun uncompressTarXZ(
        inputStream: InputStream,
        dest: File,
        updateProgress: (Int, Array<Any>) -> Unit = { _, _ -> }
    ) = withContext(Dispatchers.IO) {
        dest.ensureDirectory()
        val buffer = ByteArray(8192)

        TarArchiveInputStream(XZCompressorInputStream(inputStream)).use { tarIn ->
            generateSequence { tarIn.nextEntry }.forEach { tarEntry ->
                ensureActive()
                val tarEntryName = tarEntry.name
                updateProgress(R.string.generic_unpacking, arrayOf(tarEntryName))

                val destPath = File(dest, tarEntryName).ensureParentDirectory()

                when {
                    tarEntry.isSymbolicLink -> try {
                        Os.symlink(tarEntry.linkName, tarEntryName)
                    } catch (e: Throwable) {
                        lError("Exception occurred while creating symbolic link", e)
                    }

                    tarEntry.isDirectory -> destPath.ensureDirectory()
                    !destPath.exists() || destPath.length() != tarEntry.size ->
                        FileOutputStream(destPath).use { os ->
                            IOUtils.copyLarge(tarIn, os, buffer)
                        }
                }
            }
        }
    }
}
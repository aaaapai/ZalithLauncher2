package com.movtery.zalithlauncher.game.version.mod.reader

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.version.mod.LocalMod
import com.movtery.zalithlauncher.game.version.mod.ModMetadataReader
import com.movtery.zalithlauncher.game.version.mod.meta.LiteModMetadata
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.file.UnpackZipException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile as JDKZipFile

/**
 * [Reference HMCL](https://github.com/HMCL-dev/HMCL/blob/242df8a/HMCLCore/src/main/java/org/jackhuang/hmcl/mod/modinfo/LiteModMetadata.java)
 */
object LiteModMetadataReader : ModMetadataReader {
    override suspend fun fromLocal(modFile: File): LocalMod = withContext(Dispatchers.IO) {
        try {
            JDKZipFile(modFile).use { zip ->
                try {
                    val litemodEntry = zip.getEntry("litemod.json")
                        ?: throw IOException("File $modFile is not a LiteLoader mod.")

                    zip.getInputStream(litemodEntry).use { stream ->
                        val metadata = InputStreamReader(stream, StandardCharsets.UTF_8).use { r ->
                            GSON.fromJson(r, LiteModMetadata::class.java)
                        } ?: throw IOException("Mod $modFile `litemod.json` is malformed.")

                        return@withContext LocalMod(
                            modFile = modFile,
                            fileSize = FileUtils.sizeOf(modFile),
                            id = metadata.name,
                            loader = ModLoader.LITE_LOADER,
                            name = metadata.name,
                            description = metadata.description,
                            version = metadata.version,
                            authors = parseAuthors(metadata.author),
                            icon = null //LiteLoader 通常没有图标
                        )
                    }
                } catch (e: Exception) {
                    throw UnpackZipException(e)
                }
            }
        } catch (e: Exception) {
            if (e !is UnpackZipException) return@withContext readWithApacheZip(modFile)
            else throw e
        }
    }

    private fun readWithApacheZip(modFile: File): LocalMod {
        val zipFile = ZipFile.Builder()
            .setFile(modFile)
            .get()

        zipFile.use { zip ->
            val litemodEntry = zip.getEntry("litemod.json")
                ?: throw IOException("File $modFile is not a LiteLoader mod.")

            zip.getInputStream(litemodEntry).use { stream ->
                val metadata = InputStreamReader(stream, StandardCharsets.UTF_8).use { r ->
                    GSON.fromJson(r, LiteModMetadata::class.java)
                } ?: throw IOException("Mod $modFile `litemod.json` is malformed.")

                return LocalMod(
                    modFile = modFile,
                    fileSize = FileUtils.sizeOf(modFile),
                    id = metadata.name,
                    loader = ModLoader.LITE_LOADER,
                    name = metadata.name,
                    description = metadata.description,
                    version = metadata.version,
                    authors = parseAuthors(metadata.author),
                    icon = null //LiteLoader 通常没有图标
                )
            }
        }
    }

    private fun parseAuthors(author: String?): List<String> {
        return author?.split(',', ';', '&')?.map { it.trim() }?.filterNot { it.isBlank() }
            ?: emptyList()
    }
}
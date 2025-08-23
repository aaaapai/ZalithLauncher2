package com.movtery.zalithlauncher.game.version.mod.reader

import com.google.gson.reflect.TypeToken
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.version.mod.LocalMod
import com.movtery.zalithlauncher.game.version.mod.ModMetadataReader
import com.movtery.zalithlauncher.game.version.mod.meta.ForgeOldModMetadata
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.file.UnpackZipException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile as JDKZipFile

/**
 * [Reference HMCL](https://github.com/HMCL-dev/HMCL/blob/3eddfa2/HMCLCore/src/main/java/org/jackhuang/hmcl/mod/modinfo/ForgeOldModMetadata.java)
 */
object ForgeOldModMetadataReader : ModMetadataReader {
    override suspend fun fromLocal(modFile: File): LocalMod = withContext(Dispatchers.IO) {
        try {
            JDKZipFile(modFile).use { zip ->
                try {
                    val mcmodEntry = zip.getEntry("mcmod.info")
                        ?: throw IOException("File $modFile is not a Forge mod.")

                    zip.getInputStream(mcmodEntry).bufferedReader().use { reader ->
                        val modList: List<ForgeOldModMetadata> = GSON.fromJson(
                            reader,
                            object : TypeToken<List<ForgeOldModMetadata>>() {}.type
                        )

                        if (modList.isEmpty()) {
                            throw IOException("Mod $modFile `mcmod.info` is malformed.")
                        }

                        val metadata = modList[0]
                        val authors = determineAuthors(metadata)
                        val icon = zip.tryGetIcon(metadata.logoFile)

                        return@withContext LocalMod(
                            modFile = modFile,
                            fileSize = FileUtils.sizeOf(modFile),
                            id = metadata.modId,
                            loader = ModLoader.FORGE,
                            name = metadata.name,
                            description = metadata.description,
                            version = metadata.version,
                            authors = authors,
                            icon = icon
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
            val mcmodEntry = zip.getEntry("mcmod.info")
                ?: throw IOException("File $modFile is not a Forge mod.")

            zip.getInputStream(mcmodEntry).bufferedReader().use { reader ->
                val modList: List<ForgeOldModMetadata> = GSON.fromJson(
                    reader,
                    object : TypeToken<List<ForgeOldModMetadata>>() {}.type
                )

                if (modList.isEmpty()) {
                    throw IOException("Mod $modFile `mcmod.info` is malformed.")
                }

                val metadata = modList[0]
                val authors = determineAuthors(metadata)
                val icon = zip.tryGetIcon(metadata.logoFile)

                return LocalMod(
                    modFile = modFile,
                    fileSize = FileUtils.sizeOf(modFile),
                    id = metadata.modId,
                    loader = ModLoader.FORGE,
                    name = metadata.name,
                    description = metadata.description,
                    version = metadata.version,
                    authors = authors,
                    icon = icon
                )
            }
        }
    }

    private fun determineAuthors(metadata: ForgeOldModMetadata): List<String> {
        return when {
            metadata.authors.isNotEmpty() -> metadata.authors.toList()
            metadata.authorList.isNotEmpty() -> metadata.authorList.toList()
            metadata.author.isNotBlank() -> listOf(metadata.author)
            metadata.credits.isNotBlank() -> listOf(metadata.credits)
            else -> emptyList()
        }
    }
}
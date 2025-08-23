package com.movtery.zalithlauncher.game.version.mod

import com.movtery.zalithlauncher.game.version.mod.reader.FabricModMetadataReader
import com.movtery.zalithlauncher.game.version.mod.reader.ForgeNewModMetadataReader
import com.movtery.zalithlauncher.game.version.mod.reader.ForgeOldModMetadataReader
import com.movtery.zalithlauncher.game.version.mod.reader.LiteModMetadataReader
import com.movtery.zalithlauncher.game.version.mod.reader.PackMcMetadataReader
import com.movtery.zalithlauncher.game.version.mod.reader.QuiltModMetadataReader
import java.io.File

interface ModMetadataReader {
    suspend fun fromLocal(modFile: File): LocalMod

    companion object {
        private val NORMAL_READERS = arrayOf(
            ForgeOldModMetadataReader,
            ForgeNewModMetadataReader,
            FabricModMetadataReader,
            QuiltModMetadataReader,
            PackMcMetadataReader
        )

        val READERS = mapOf<String, Array<ModMetadataReader>>(
            "zip" to NORMAL_READERS,
            "jar" to NORMAL_READERS,
            "litemod" to arrayOf(
                LiteModMetadataReader
            )
        )
    }
}
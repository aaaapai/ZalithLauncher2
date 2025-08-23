package com.movtery.zalithlauncher.game.download.modpack.platform.curseforge

import com.google.gson.annotations.SerializedName

class CurseForgeManifest(
    val manifestType: String,
    val manifestVersion: Int,
    val name: String,
    val version: String,
    val author: String,
    val overrides: String? = null,
    val minecraft: Minecraft,
    val files: List<ManifestFile>
) {
    data class Minecraft(
        @SerializedName("version")
        val gameVersion: String,
        val modLoaders: List<ModLoader>
    ) {
        data class ModLoader(
            val id: String,
            val primary: Boolean
        )
    }

    data class ManifestFile(
        val projectID: Int,
        val fileID: Int,
        val fileName: String? = null,
        val url: String? = null,
        val required: Boolean
    ) {
        fun getFileUrl(): String? {
            return url ?: fileName?.let {
                "https://edge.forgecdn.net/files/${fileID / 1000}/${fileID % 1000}/$it"
            }
        }
    }
}
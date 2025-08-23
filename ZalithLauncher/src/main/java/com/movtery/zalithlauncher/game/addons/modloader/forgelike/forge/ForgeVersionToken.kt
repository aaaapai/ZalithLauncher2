package com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge

import kotlinx.serialization.Serializable

@Serializable
data class ForgeVersionToken(
    val branch: String? = null,
    val version: String,
    val modified: String,
    val files: List<ForgeFile>
) {
    @Serializable
    data class ForgeFile(
        val category: String,
        val format: String,
        val hash: String
    ) {
        companion object {
            fun ForgeFile.isInstallerJar() = category == "installer" && format == "jar"
            fun ForgeFile.isUniversalZip() = category == "universal" && format == "zip"
            fun ForgeFile.isClientZip() = category == "client" && format == "zip"
        }
    }
}
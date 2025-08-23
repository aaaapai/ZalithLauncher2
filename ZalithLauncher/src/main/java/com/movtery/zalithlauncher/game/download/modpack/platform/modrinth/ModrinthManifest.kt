package com.movtery.zalithlauncher.game.download.modpack.platform.modrinth

class ModrinthManifest(
    val game: String,
    val formatVersion: Int,
    val versionId: String,
    val name: String,
    /** optional */
    val summary: String? = null,
    val files: Array<ManifestFile>,
    val dependencies: Map<String, String>
) {
    class ManifestFile(
        val path: String,
        val hashes: Hashes,
        /** optional */
        val env: Env? = null,
        val downloads: Array<String>,
        val fileSize: Long
    ) {
        class Hashes(
            val sha1: String,
            val sha512: String
        )

        class Env(
            val client: String,
            val server: String
        )
    }
}
package com.movtery.zalithlauncher.game.version.mod.meta

data class ForgeNewModMetadata(
    val modLoader: String? = null,
    val loaderVersion: String? = null,
    val logoFile: String? = null,
    val license: String? = null,
    val mods: List<Mod> = emptyList()
) {
    data class Mod(
        val modId: String,
        val version: String,
        val displayName: String,
        val side: String? = null,
        val displayURL: String? = null,
        val authors: List<String>? = null,
        val description: String? = null
    )
}
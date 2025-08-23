package com.movtery.zalithlauncher.game.version.mod.meta

data class LiteModMetadata(
    val name: String,
    val version: String,
    val mcversion: String,
    val revision: String? = null,
    val author: String? = null,
    val classTransformerClasses: Array<String>? = null,
    val description: String? = null,
    val modpackName: String? = null,
    val modpackVersion: String? = null,
    val checkUpdateUrl: String? = null,
    val updateURI: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LiteModMetadata
        return classTransformerClasses.contentEquals(other.classTransformerClasses)
    }

    override fun hashCode(): Int {
        return classTransformerClasses?.contentHashCode() ?: 0
    }
}
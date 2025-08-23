package com.movtery.zalithlauncher.game.version.mod.meta

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class QuiltModMetadata(
    @SerializedName("schema_version")
    val schemaVersion: Int,
    @SerializedName("quilt_loader")
    val quiltLoader: QuiltLoader
) {
    data class QuiltLoader(
        val id: String,
        val version: String,
        val metadata: Metadata
    ) {
        data class Metadata(
            val name: String,
            val description: String,
            val contributors: JsonObject? = null,
            val icon: String? = null,
            val contact: JsonObject? = null
        )
    }
}
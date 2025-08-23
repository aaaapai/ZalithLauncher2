package com.movtery.zalithlauncher.game.version.mod.meta

import com.google.gson.annotations.SerializedName

data class ForgeOldModMetadata(
    @SerializedName("modid")
    val modId: String = "",
    val name: String = "",
    val description: String = "",
    val author: String = "",
    val version: String = "",
    val logoFile: String = "",
    @SerializedName("mcversion")
    val gameVersion: String = "",
    val url: String = "",
    val updateUrl: String = "",
    val credits: String = "",
    val authorList: List<String> = emptyList(),
    val authors: List<String> = emptyList()
)
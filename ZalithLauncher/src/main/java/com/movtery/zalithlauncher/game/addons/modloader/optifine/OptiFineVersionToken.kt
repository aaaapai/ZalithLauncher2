package com.movtery.zalithlauncher.game.addons.modloader.optifine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OptiFineVersionToken(
    @SerialName("mcversion")
    val mcVersion: String,
    val type: String,
    val patch: String,
    @SerialName("filename")
    val fileName: String,
    val forge: String? = null
)
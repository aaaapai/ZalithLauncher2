package com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ModrinthProjectType {
    @SerialName("mod")
    MOD,

    @SerialName("modpack")
    MODPACK,

    @SerialName("resourcepack")
    RESOURCEPACK,

    @SerialName("shader")
    SHADER
}
package com.movtery.zalithlauncher.game.download.assets.platform.mcmod.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class McModSearch (
    @SerialName("type")
    val type: Int,
    @SerialName("ids")
    val ids: Set<String>,
    @SerialName("mcmod_type")
    val mcmod_type: Int
)
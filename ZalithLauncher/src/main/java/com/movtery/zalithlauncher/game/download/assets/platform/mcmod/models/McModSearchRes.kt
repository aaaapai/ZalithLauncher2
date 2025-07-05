package com.movtery.zalithlauncher.game.download.assets.platform.mcmod.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class McModSearchRes (
    @SerialName("res")
    val res: Int,
    @SerialName("data")
    val data: Map<String, McModSearchItem?>?,
    @SerialName("text")
    val text: String?
)
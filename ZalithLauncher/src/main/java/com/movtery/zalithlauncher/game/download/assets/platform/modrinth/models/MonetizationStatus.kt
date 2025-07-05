package com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MonetizationStatus {
    @SerialName("monetized")
    MONETIZED,

    @SerialName("demonetized")
    DEMONETIZED,

    @SerialName("force-demonetized")
    FORCE_DEMONETIZED
}
package com.movtery.zalithlauncher.game.addons.modloader.fabriclike.models

import kotlinx.serialization.Serializable

@Serializable
data class FabricLikeVersionsJson(
    val game: List<FabricLikeGame>,
    val loader: List<FabricLikeLoader>
)

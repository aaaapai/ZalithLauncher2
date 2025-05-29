package com.movtery.zalithlauncher.game.account.auth_server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Refresh(
    @SerialName("selectedProfile")
    var selectedProfile: SelectedProfile? = null,
    @SerialName("accessToken")
    var accessToken: String,
    @SerialName("clientToken")
    var clientToken: String
) {
    @Serializable
    class SelectedProfile(
        @SerialName("name")
        var name: String,
        @SerialName("id")
        var id: String
    )
}
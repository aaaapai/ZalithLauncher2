package com.movtery.zalithlauncher.game.account.auth_server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class AuthResult(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("clientToken")
    var clientToken: String,
    @SerialName("availableProfiles")
    var availableProfiles: List<AvailableProfiles>? = null,
    @SerialName("user")
    var user: User? = null,
    @SerialName("selectedProfile")
    var selectedProfile: SelectedProfile? = null
) {
    @Serializable
    class User(
        @SerialName("id")
        var id: String,
        @SerialName("properties")
        var properties: JsonElement? = null
    )

    @Serializable
    class SelectedProfile(
        @SerialName("id")
        var id: String,
        @SerialName("name")
        var name: String
    )

    @Serializable
    class AvailableProfiles(
        @SerialName("id")
        var id: String,
        @SerialName("name")
        var name: String
    )
}
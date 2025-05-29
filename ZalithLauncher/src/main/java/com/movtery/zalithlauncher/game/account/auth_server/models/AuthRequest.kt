package com.movtery.zalithlauncher.game.account.auth_server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AuthRequest(
    @SerialName("agent")
    var agent: Agent,
    @SerialName("username")
    var username: String,
    @SerialName("password")
    var password: String,
    @SerialName("clientToken")
    var clientToken: String,
    @SerialName("requestUser")
    var requestUser: Boolean
) {
    @Serializable
    class Agent(
        @SerialName("name")
        var name: String,
        @SerialName("version")
        var version: Int
    )
}
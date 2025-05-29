package com.movtery.zalithlauncher.game.account.auth_server

class ResponseException(val responseMessage: String) : RuntimeException(
    responseMessage
)

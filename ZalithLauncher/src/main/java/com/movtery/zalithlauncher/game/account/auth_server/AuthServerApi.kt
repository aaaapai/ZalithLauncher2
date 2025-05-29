package com.movtery.zalithlauncher.game.account.auth_server

import android.content.Context
import com.google.gson.Gson
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.auth_server.models.AuthRequest
import com.movtery.zalithlauncher.game.account.auth_server.models.AuthResult
import com.movtery.zalithlauncher.game.account.auth_server.models.Refresh
import com.movtery.zalithlauncher.path.UrlManager.Companion.GLOBAL_CLIENT
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.string.StringUtils
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.util.Objects
import java.util.UUID

object AuthServerApi {
    private var baseUrl: String? = null

    fun setBaseUrl(baseUrl: String) {
        var url = baseUrl
        if (baseUrl.endsWith("/")) {
            url = baseUrl.dropLast(1)
        }
        AuthServerApi.baseUrl = url
    }

    @Throws(IOException::class)
    suspend fun login(
        context: Context,
        userName: String,
        password: String,
        onSuccess: suspend (AuthResult) -> Unit = {},
        onFailed: suspend (th: Throwable) -> Unit = {}
    ) {
        if (Objects.isNull(baseUrl)) {
            onFailed(ResponseException(context.getString(R.string.account_other_login_baseurl_not_set)))
            return
        }

        val agent = AuthRequest.Agent(
            name = "Minecraft",
            version = 1
        )

        val authRequest = AuthRequest(
            username = userName,
            password = password,
            agent = agent,
            requestUser = true,
            clientToken = UUID.randomUUID().toString().replace("-", "")
        )

        val data = Gson().toJson(authRequest)
        callLogin(data, "/authserver/authenticate", onSuccess, onFailed)
    }

    @Throws(IOException::class)
    suspend fun refresh(
        context: Context,
        account: Account,
        select: Boolean,
        onSuccess: suspend (AuthResult) -> Unit = {},
        onFailed: suspend (th: Throwable) -> Unit = {}
    ) {
        if (Objects.isNull(baseUrl)) {
            onFailed(ResponseException(context.getString(R.string.account_other_login_baseurl_not_set)))
            return
        }

        val refresh = Refresh(
            clientToken = account.clientToken,
            accessToken = account.accessToken
        )

        if (select) {
            refresh.selectedProfile = Refresh.SelectedProfile(
                name = account.username,
                id = account.profileId
            )
        }

        val json = Gson().toJson(refresh)
        callLogin(json, "/authserver/refresh", onSuccess, onFailed)
    }

    private suspend fun callLogin(
        data: String,
        url: String,
        onSuccess: suspend (AuthResult) -> Unit = {},
        onFailed: suspend (th: Throwable) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = GLOBAL_CLIENT.post(baseUrl + url) {
                contentType(ContentType.Application.Json)
                setBody(data)
            }

            if (response.status == HttpStatusCode.OK) {
                val result: AuthResult = response.body()
                onSuccess(result)
            } else {
                val errorMessage = "(${response.status.value}) ${parseError(response)}"
                lError(errorMessage)
                onFailed(ResponseException(errorMessage))
            }
        } catch (e: CancellationException) {
            lDebug("Login cancelled")
        } catch (e: Exception) {
            lError("Request failed", e)
            onFailed(e)
        }
    }

    private suspend fun parseError(response: HttpResponse): String {
        return try {
            val res = response.bodyAsText()
            val json = JSONObject(res)
            var message = when {
                json.has("errorMessage") -> json.getString("errorMessage")
                json.has("message") -> json.getString("message")
                else -> "Unknown error"
            }
            if (message.contains("\\u")) {
                message = StringUtils.decodeUnicode(message.replace("\\\\u", "\\u"))
            }
            message
        } catch (e: Exception) {
            lError("Failed to parse error", e)
            "Unknown error"
        }
    }

    suspend fun getServeInfo(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = GLOBAL_CLIENT.get(url)
            if (response.status == HttpStatusCode.OK) {
                response.bodyAsText()
            } else {
                null
            }
        } catch (e: Exception) {
            lError("Failed to get server info", e)
            null
        }
    }
}
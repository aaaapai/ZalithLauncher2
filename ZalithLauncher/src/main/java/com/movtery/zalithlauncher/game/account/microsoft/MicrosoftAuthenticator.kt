package com.movtery.zalithlauncher.game.account.microsoft

import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.AccountType
import com.movtery.zalithlauncher.game.account.AccountsManager
import com.movtery.zalithlauncher.game.account.microsoft.MinecraftProfileException.ExceptionStatus.BLOCKED_IP
import com.movtery.zalithlauncher.game.account.microsoft.MinecraftProfileException.ExceptionStatus.FREQUENT
import com.movtery.zalithlauncher.game.account.microsoft.MinecraftProfileException.ExceptionStatus.PROFILE_NOT_EXISTS
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.BANNED
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.BLOCKED_REGION
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.NOT_ACCEPTED_SERVICE
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.REACHED_PLAYTIME_LIMIT
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.REQUIRES_PROOF_OF_AGE
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.RESTRICTED
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.UNDERAGE
import com.movtery.zalithlauncher.game.account.microsoft.XboxLoginException.ExceptionStatus.UNREGISTERED
import com.movtery.zalithlauncher.game.account.microsoft.models.DeviceCodeResponse
import com.movtery.zalithlauncher.game.account.microsoft.models.MinecraftAuthResponse
import com.movtery.zalithlauncher.game.account.microsoft.models.TokenResponse
import com.movtery.zalithlauncher.game.account.microsoft.models.XBLProperties
import com.movtery.zalithlauncher.game.account.microsoft.models.XBLRequest
import com.movtery.zalithlauncher.game.account.microsoft.models.XSTSAuthResult
import com.movtery.zalithlauncher.game.account.microsoft.models.XSTSProperties
import com.movtery.zalithlauncher.game.account.microsoft.models.XSTSRequest
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.path.UrlManager.Companion.GLOBAL_CLIENT
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import com.movtery.zalithlauncher.utils.network.httpPostJson
import com.movtery.zalithlauncher.utils.network.submitForm
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.coroutines.CoroutineContext

object MicrosoftAuthenticator {
    private val SCOPES = listOf("XboxLive.signin", "offline_access", "openid", "profile", "email")
    private const val TENANT = "/consumers"

    private const val MICROSOFT_AUTH_URL = "https://login.microsoftonline.com"
    private const val LIVE_AUTH_URL = "https://login.live.com"
    private const val XBL_AUTH_URL = "https://user.auth.xboxlive.com"
    private const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com"
    private const val MINECRAFT_SERVICES_URL = "https://api.minecraftservices.com"

    /**
     * 从 Microsoft 身份验证终端节点获取设备代码响应
     * 设备代码用于在单独的设备或浏览器上授权用户
     */
    suspend fun fetchDeviceCodeResponse(context: CoroutineContext): DeviceCodeResponse = coroutineScope {
        withRetry {
            submitForm(
                url = "$MICROSOFT_AUTH_URL/$TENANT/oauth2/v2.0/devicecode",
                parameters = Parameters.build {
                    append("client_id", InfoDistributor.OAUTH_CLIENT_ID)
                    append("scope", SCOPES.joinToString(" "))
                },
                context = context
            )
        }
    }

    /**
     * 使用设备代码流从 Microsoft Azure Active Directory 检索访问令牌和刷新令牌
     * 此函数会定期轮询 Microsoft 令牌端点，直到获取访问令牌或超时
     */
    suspend fun getTokenResponse(
        codeResponse: DeviceCodeResponse,
        context: CoroutineContext,
        checkCancelled: () -> Boolean
    ): TokenResponse = coroutineScope {
        var pollingInterval = codeResponse.interval * 1000L
        val expireTime = System.currentTimeMillis() + codeResponse.expiresIn * 1000L

        var cancelled = 0
        fun checkIsReallyCancelled(): Boolean {
            if (checkCancelled()) cancelled++
            return cancelled > 1
        }

        while (System.currentTimeMillis() < expireTime) {
            context.ensureActive()
            if (checkIsReallyCancelled()) throw CancellationException("Authentication cancelled")

            try {
                val response: JsonObject = submitForm(
                    "$MICROSOFT_AUTH_URL$TENANT/oauth2/v2.0/token",
                    parameters = Parameters.build {
                        append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                        append("device_code", codeResponse.deviceCode)
                        append("client_id", InfoDistributor.OAUTH_CLIENT_ID)
                        append("tenant", TENANT)
                    },
                    context = context
                )

                if (response["token_type"]?.jsonPrimitive?.content == "Bearer") {
                    return@coroutineScope TokenResponse(
                        accessToken = response["access_token"].text(),
                        refreshToken = response["refresh_token"].text(),
                        expiresIn = response["expires_in"]?.jsonPrimitive?.int ?: 0
                    )
                }
            } catch (e: ClientRequestException) {
                handleClientRequestException(e, pollingInterval)
                pollingInterval = adjustPollingInterval(e, pollingInterval)
            } catch (e: CancellationException) {
                lDebug("Authentication cancelled")
                throw e
            }
            delay(pollingInterval).also {
                context.ensureActive()
                if (checkIsReallyCancelled()) throw CancellationException("Authentication cancelled")
            }
        }
        throw HttpRequestTimeoutException("Authentication timed out!", expireTime)
    }

    private suspend fun handleClientRequestException(e: ClientRequestException, interval: Long) {
        val errorBody = Json.parseToJsonElement(e.response.bodyAsText()).jsonObject
        when (errorBody["error"]?.jsonPrimitive?.content) {
            "authorization_pending" -> Unit /* 正常情况，继续轮询 */
            "slow_down" -> lDebug("Slowing down polling to ${interval + 1000}ms")
            else -> throw e
        }
    }

    private suspend fun adjustPollingInterval(e: ClientRequestException, currentInterval: Long): Long {
        return if (e.isSlowDownError()) currentInterval + 1000L else currentInterval
    }

    private suspend fun ClientRequestException.isSlowDownError(): Boolean {
        val error = Json.parseToJsonElement(response.bodyAsText())
            .jsonObject["error"]?.jsonPrimitive?.content
        return error == "slow_down"
    }

    /**
     * 使用不同的身份验证类型异步验证用户，并检索其 Minecraft 帐户信息
     * 函数通过执行一系列步骤来编排身份验证过程，具体取决于提供的 [authType]。
     *
     * 支持刷新现有访问令牌或使用提供的访问令牌。然后，继续使用 Xbox Live （XBL）、Xbox 安全令牌服务 （XSTS） 进行身份验证，最后访问 Minecraft。
     *
     * 支持验证用户是否拥有游戏，然后创建 [Account] 对象。
     *
     * @param statusUpdate 验证执行到哪个步骤，通过这个进行回调更新
     */
    suspend fun authAsync(
        authType: AuthType,
        refreshToken: String,
        accessToken: String = "NULL",
        context: CoroutineContext,
        statusUpdate: (AsyncStatus) -> Unit,
    ): Account = coroutineScope {
        val (finalAccessToken, newRefreshToken) = when (authType) {
            AuthType.Refresh -> refreshAccessToken(refreshToken, statusUpdate, context)
            else -> Pair(accessToken, refreshToken)
        }

        val xblToken = authenticateXBL(finalAccessToken, statusUpdate)
        val xstsToken = authenticateXSTS(xblToken.first, xblToken.second, statusUpdate, context)
        val minecraftToken = authenticateMinecraft(xstsToken, statusUpdate, context)
        verifyGameOwnership(minecraftToken, statusUpdate)

        return@coroutineScope createAccount(minecraftToken, newRefreshToken, xblToken.second, statusUpdate)
    }

    private suspend fun refreshAccessToken(
        refreshToken: String,
        update: (AsyncStatus) -> Unit,
        context: CoroutineContext
    ): Pair<String, String> {
        update(AsyncStatus.GETTING_ACCESS_TOKEN)

        return withRetry {
            val response = submitForm<JsonObject>(
                url = "$LIVE_AUTH_URL/oauth20_token.srf",
                parameters = Parameters.build {
                    append("client_id", InfoDistributor.OAUTH_CLIENT_ID)
                    append("refresh_token", refreshToken)
                    append("grant_type", "refresh_token")
                },
                context = context
            )
            Pair(
                response["access_token"].text(),
                response["refresh_token"]?.jsonPrimitive?.content ?: refreshToken
            )
        }
    }

    private suspend fun authenticateXBL(accessToken: String, update: (AsyncStatus) -> Unit): Pair<String, String> {
        update(AsyncStatus.GETTING_XBL_TOKEN)
        val requestBody = XBLRequest(
            properties = XBLProperties(
                authMethod = "RPS",
                siteName = "user.auth.xboxlive.com",
                rpsTicket = "d=$accessToken"
            ),
            relyingParty = "http://auth.xboxlive.com",
            tokenType = "JWT"
        )

        return withRetry {
            val response = GLOBAL_CLIENT.post("$XBL_AUTH_URL/user/authenticate") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body<JsonObject>()

            //提取uhs
            val uhs = response["DisplayClaims"]?.jsonObject
                ?.get("xui")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("uhs")?.jsonPrimitive
                ?.content ?: throw Exception("Missing uhs in XBL response")

            Pair(response["Token"].text(), uhs)
        }
    }

    private suspend fun authenticateXSTS(
        xblToken: String,
        uhs: String,
        update: (AsyncStatus) -> Unit,
        context: CoroutineContext
    ): XSTSAuthResult {
        update(AsyncStatus.GETTING_XSTS_TOKEN)

        return withRetry {
            val response = httpPostJson<JsonObject>(
                url = "$XSTS_AUTH_URL/xsts/authorize",
                body = XSTSRequest(
                    properties = XSTSProperties(
                        sandboxId = "RETAIL",
                        userTokens = listOf(xblToken)
                    ),
                    relyingParty = "rp://api.minecraftservices.com/",
                    tokenType = "JWT"
                ),
                context = context
            )

            when (response["XErr"].text()) {
                //Reference : https://github.com/PrismarineJS/prismarine-auth/blob/1aef6e1/src/common/Constants.js#L50-L59
                "2148916227" -> throw XboxLoginException(BANNED)
                "2148916229" -> throw XboxLoginException(RESTRICTED)
                "2148916233" -> throw XboxLoginException(UNREGISTERED)
                "2148916234" -> throw XboxLoginException(NOT_ACCEPTED_SERVICE)
                "2148916235" -> throw XboxLoginException(BLOCKED_REGION)
                "2148916236" -> throw XboxLoginException(REQUIRES_PROOF_OF_AGE)
                "2148916237" -> throw XboxLoginException(REACHED_PLAYTIME_LIMIT)
                "2148916238" -> throw XboxLoginException(UNDERAGE)
            }

            XSTSAuthResult(token = response["Token"].text(), uhs = uhs)
        }
    }

    private suspend fun authenticateMinecraft(
        xstsResult: XSTSAuthResult,
        update: (AsyncStatus) -> Unit,
        context: CoroutineContext
    ): String {
        update(AsyncStatus.AUTHENTICATE_MINECRAFT)

        return withRetry {
            runCatching {
                httpPostJson<MinecraftAuthResponse>(
                    url = "$MINECRAFT_SERVICES_URL/authentication/login_with_xbox",
                    body = mapOf("identityToken" to "XBL3.0 x=${xstsResult.uhs};${xstsResult.token}"),
                    context = context
                )
            }.onFailure { e ->
                if (e is ResponseException) {
                    when (e.response.status.value) {
                        429 -> throw MinecraftProfileException(FREQUENT)
                        403 -> throw MinecraftProfileException(BLOCKED_IP)
                    }
                }
            }.getOrThrow()
                .accessToken
        }
    }

    private suspend fun verifyGameOwnership(accessToken: String, update: (AsyncStatus) -> Unit) {
        update(AsyncStatus.VERIFY_GAME_OWNERSHIP)
        withRetry {
            val response = GLOBAL_CLIENT.get("$MINECRAFT_SERVICES_URL/entitlements/mcstore") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
            if (Json.parseToJsonElement(response.bodyAsText()).jsonObject["items"]?.jsonArray?.isEmpty() != false) {
                throw NotPurchasedMinecraftException()
            }
        }
    }

    private suspend fun createAccount(
        accessToken: String,
        refreshToken: String,
        uhs: String,
        statusUpdate: (AsyncStatus) -> Unit
    ): Account {
        statusUpdate(AsyncStatus.GETTING_PLAYER_PROFILE)

        val profile = runCatching {
            GLOBAL_CLIENT.get("$MINECRAFT_SERVICES_URL/minecraft/profile") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }.body<JsonObject>()
        }.onFailure { e ->
            if (e is ResponseException) {
                when (e.response.status.value) {
                    429 -> throw MinecraftProfileException(FREQUENT)
                    404 -> throw MinecraftProfileException(PROFILE_NOT_EXISTS)
                }
            }
        }.getOrThrow()

        val profileId = profile["id"].text()
        //避免同一个账号反复添加
        val account = AccountsManager.loadFromProfileID(profileId) ?: Account()

        return account.apply {
            this.username = profile["name"].text()
            this.accessToken = accessToken
            this.accountType = AccountType.MICROSOFT.tag
            this.clientToken = UUID.randomUUID().toString().replace("-", "")
            this.profileId = profileId
            this.refreshToken = refreshToken.ifEmpty { "None" }
            this.xUid = uhs
        }
    }

    private fun JsonElement?.text() = this?.jsonPrimitive?.content.orEmpty()

    private suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10_000,
        block: suspend () -> T
    ): T = com.movtery.zalithlauncher.utils.network.withRetry(
        "MicrosoftAuthenticator", maxRetries, initialDelay, maxDelay, block
    )
}
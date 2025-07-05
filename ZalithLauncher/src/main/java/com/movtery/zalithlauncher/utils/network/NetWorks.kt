package com.movtery.zalithlauncher.utils.network

import com.movtery.zalithlauncher.path.UrlManager.Companion.GLOBAL_CLIENT
import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.CoroutineContext

suspend inline fun <reified T> submitForm(
    url: String,
    parameters: Parameters,
    context: CoroutineContext = Dispatchers.IO
): T = withContext(context) {
    GLOBAL_CLIENT.submitForm(
        url = url,
        formParameters = parameters
    ) {
        contentType(ContentType.Application.FormUrlEncoded)
    }.body()
}

suspend inline fun <reified T> httpPostJson(
    url: String,
    headers: List<Pair<String, Any?>>? = null,
    body: Any,
    context: CoroutineContext = Dispatchers.IO
): T = withContext(context) {
    GLOBAL_CLIENT.post(url) {
        contentType(ContentType.Application.Json)
        headers?.takeIf { it.isNotEmpty() }?.forEach { (k, v) -> header(k, v) }
        setBody(body)
    }.body()
}

suspend inline fun <reified T> httpGet(
    url: String,
    headers: List<Pair<String, Any?>>? = null,
    parameters: Parameters? = null,
    context: CoroutineContext = Dispatchers.IO
): T = withContext(context) {
    GLOBAL_CLIENT.get(url) {
        headers?.takeIf { it.isNotEmpty() }?.forEach { (k, v) -> header(k, v) }
        parameters?.let { value ->
            url {
                this.parameters.appendAll(value)
            }
        }
    }.body()
}

suspend fun <T> withRetry(
    logTag: String,
    maxRetries: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10_000,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    var retryCount = 0
    var lastError: Throwable? = null

    while (retryCount < maxRetries) {
        try {
            return block()
        } catch (e: CancellationException) {
            //协程被取消时不重试，直接抛出
            lDebug("$logTag: Cancelled: ${e.message}")
            throw e
        } catch (e: Exception) {
            lDebug("$logTag: Attempt ${retryCount + 1} failed: ${e.message}")
            lastError = e
            if (canRetry(e)) {
                delay(currentDelay)
                currentDelay = (currentDelay * 2).coerceAtMost(maxDelay)
                retryCount++
            } else {
                throw e //不可重试
            }
        }
    }
    throw lastError ?: Exception("Failed after $maxRetries retries")
}

private fun canRetry(e: Exception): Boolean {
    return when (e) {
        is ClientRequestException -> e.response.status.value in 500..599 //5xx错误可重试
        is IOException -> true //网络错误
        else -> false
    }
}
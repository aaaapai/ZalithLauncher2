package com.movtery.zalithlauncher.utils.network

import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.delay
import java.io.IOException

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
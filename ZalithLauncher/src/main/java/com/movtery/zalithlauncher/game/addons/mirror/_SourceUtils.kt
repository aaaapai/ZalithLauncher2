package com.movtery.zalithlauncher.game.addons.mirror

import com.movtery.zalithlauncher.utils.logging.Logger.lDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

data class MirrorSource<T>(
    val delayMillis: Long = 0L,
    val type: SourceType,
    val block: suspend () -> T?
)

suspend fun <T> runMirrorable(
    sources: List<MirrorSource<T>>
): T? = withContext(Dispatchers.IO) {
    var result: T? = null
    var succeed = false
    var lastException: Throwable? = null

    loop@ for (source in sources) {
        ensureActive()
        if (source.delayMillis > 0) {
            delay(source.delayMillis)
        }
        ensureActive()

        runCatching {
            val res = source.block()
            result = res
            succeed = true
            break@loop
        }.onFailure {
            lDebug("Source ${source.type.displayName} failed!", it)
            lastException = it
        }
    }

    if (!succeed) throw lastException ?: IOException("Failed to retrieve information from the source!")

    result
}
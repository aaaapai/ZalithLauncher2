package com.movtery.zalithlauncher.terracotta

import com.google.gson.JsonParseException
import com.movtery.zalithlauncher.path.GLOBAL_CLIENT
import com.movtery.zalithlauncher.utils.isChinaMainland
import com.movtery.zalithlauncher.utils.isChinese
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.network.safeBodyAsJson
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URI
import java.net.URISyntaxException

private const val NODE_LIST_URL = "https://terracotta.glavo.site/nodes"

@Serializable
private data class TerracottaNode(
    @SerialName("url")
    val url: String? = null,
    @SerialName("region")
    val region: String? = null
) {
    fun validate() {
        requireNotNull(url, { "TerracottaNode.url cannot be null" })
        try {
            URI(url)
        } catch (e: URISyntaxException) {
            throw JsonParseException("Invalid URL: $url", e)
        }
    }
}

@Volatile
private var nodeList: List<URI>? = null

private val fetchMutex = Mutex()

/**
 * [Reference HMCL](https://github.com/HMCL-dev/HMCL/blob/a70b8d7/HMCL/src/main/java/org/jackhuang/hmcl/terracotta/TerracottaNodeList.java)
 */
suspend fun fetchNodes(): List<URI> {
    nodeList?.let { return it }

    return fetchMutex.withLock {
        nodeList?.let { return it }

        withContext(Dispatchers.IO) {
            fetchNodesFromRemote()
        }.also { result ->
            nodeList = result
        }
    }
}

private suspend fun fetchNodesFromRemote(): List<URI> {
    return runCatching {
        val nodes = GLOBAL_CLIENT
            .get(NODE_LIST_URL)
            .safeBodyAsJson<List<TerracottaNode?>?>()

        if (nodes.isNullOrEmpty()) {
            lInfo("No available Terracotta nodes found")
            return emptyList()
        }

        val result = nodes
            .asSequence()
            .mapNotNull { node ->
                if (node == null) return@mapNotNull null
                parseNode(node)
            }
            .toList()

        lInfo("Terracotta node list: $result")
        result
    }.onFailure {
        lWarning("Failed to fetch terracotta node list", it)
    }.getOrDefault(emptyList())
}

private fun parseNode(
    node: TerracottaNode
): URI? {
    return try {
        node.validate()

        if (!shouldUseNode(node.region)) {
            return null
        }

        URI.create(node.url)

    } catch (e: Exception) {
        lWarning("Invalid terracotta node: $node", e)
        null
    }
}

private fun shouldUseNode(
    region: String?
): Boolean {
    if (region.isNullOrBlank()) return true
    if (!isChinese()) return false

    //仅限中国大陆地区使用
    val isMainLand = isChinaMainland()
    return isMainLand && region.equals("CN", ignoreCase = true)
}

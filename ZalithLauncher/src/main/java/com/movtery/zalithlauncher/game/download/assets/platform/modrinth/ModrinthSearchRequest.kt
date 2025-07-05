package com.movtery.zalithlauncher.game.download.assets.platform.modrinth

import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSortField
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthFacet
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ProjectTypeFacet
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.toFacetsString
import io.ktor.http.Parameters

/**
 * [Modrinth api](https://docs.modrinth.com/api/operations/searchprojects/)
 */
data class ModrinthSearchRequest(
    /** 搜索词条 */
    val query: String = "",

    /** 应用于搜索的过滤器列表 */
    val facets: List<ModrinthFacet> = listOf(ProjectTypeFacet.MOD),

    /** 排序方式 */
    val index: PlatformSortField = PlatformSortField.RELEVANCE,

    /** 要跳过的结果页数（用于分页） */
    val offset: Int = 0,

    /** 要返回的结果页数，最大值为 100 */
    val limit: Int = 20
) {
    /**
     * 转换为 GET 参数
     */
    fun toParameters(): Parameters = Parameters.build {
        append("query", query)
        append("facets", facets.toFacetsString())
        append("limit", limit.toString())
        append("index", index.modrinth)
        append("offset", offset.toString())
    }
}
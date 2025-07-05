package com.movtery.zalithlauncher.game.download.assets.platform.curseforge

import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSortField
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeCategory
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeClassID
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeModLoader
import io.ktor.http.Parameters

/**
 * [CurseForge api](https://docs.curseforge.com/rest-api/?shell#search-mods)
 */
data class CurseForgeSearchRequest(
    /** 游戏 ID */
    val gameId: Int = 432, //Minecraft 游戏 ID

    val classId: Int = CurseForgeClassID.MOD.classID,

    /** 搜索的资源的类别 */
    val categories: Set<CurseForgeCategory>? = null,

    /** 资源名称过滤搜索 */
    val searchFilter: String? = null,

    /** 游戏版本过滤 */
    val gameVersion: String? = null,

    /** 排序方式 */
    val sortField: PlatformSortField = PlatformSortField.RELEVANCE,

    val sortOrder: String = "desc",

    /** 模组加载器过滤 */
    val modLoader: CurseForgeModLoader? = null,

    /** 要跳过的结果页数（用于分页） */
    val index: Int = 0,

    /** 要返回的结果页数，最大值为 50 */
    val pageSize: Int = 20,
) {
    /**
     * 转换为 GET 参数
     */
    fun toParameters(): Parameters = Parameters.build {
        append("gameId", gameId.toString())
        append("classId", classId.toString())
        categories.mutableParameters(
            singleName = "categoryId",
            mutableName = "categoryIds",
            toString = { it.describe() },
        ) { name, value ->
            append(name, value)
        }
        searchFilter?.let {
            append("searchFilter", it)
        }
        gameVersion?.let {
            append("gameVersion", it)
        }
        modLoader?.let {
            append("modLoaderType", it.code.toString())
        }
        append("sortField", sortField.curseforge)
        append("sortOrder", sortOrder)
        append("index", index.toString())
        append("pageSize", pageSize.toString())
    }

    /**
     * 分情况选择不同的参数名，处理参数列表为字符串值
     * 目前只有 categoryIds 能够正常使用
     */
    private fun <E> Collection<E>?.mutableParameters(
        singleName: String,
        mutableName: String,
        toString: E.(E) -> String?,
        append: (name: String, value: String) -> Unit
    ) {
        this?.takeIf { it.isNotEmpty() }?.let { c1 ->
            if (c1.size > 1) {
                c1.mapNotNull { it.toString(it) }
                    .takeIf { it.isNotEmpty() }
                    ?.let { categoryStrings ->
                        //官方文档没有细说应该如何配置（头大）
                        //多过滤器的配置方式：name=[aaa,bbb,...]
                        append(mutableName, categoryStrings.joinToString(separator = ",", prefix = "[", postfix = "]") { it })
                    }
            } else {
                val value = c1.first()
                value.toString(value)
                    .takeIf { !it.isNullOrEmpty() }
                    ?.let { categoryString ->
                        append(singleName, categoryString)
                    }
            }
        }
    }
}
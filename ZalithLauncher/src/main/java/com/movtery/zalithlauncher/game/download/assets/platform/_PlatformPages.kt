package com.movtery.zalithlauncher.game.download.assets.platform

import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.CurseForgeSearchResult
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.ModrinthSearchResult
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.AssetsPage

/**
 * 获取分页信息
 */
fun PlatformSearchResult.getPageInfo(
    block: (pageNumber: Int, pageIndex: Int, totalPage: Int, isLastPage: Boolean) -> Unit
) {
    when (this) {
        is CurseForgeSearchResult -> {
            val pagination = this.pagination
            val pageSize = pagination.pageSize
            val totalPage = ((pagination.totalCount + pageSize - 1) / pageSize).toInt()
            val pageNumber = pagination.index / pageSize + 1
            val isLastPage = pagination.resultCount < pageSize ||
                    (pagination.index + pagination.resultCount) >= pagination.totalCount
            block(pageNumber, pagination.index, totalPage, isLastPage)
        }

        is ModrinthSearchResult -> {
            val pageSize = this.limit
            val totalPage = ((this.totalHits + pageSize - 1) / pageSize).toInt()
            val pageNumber = this.offset / pageSize + 1
            val isLastPage = (this.offset + this.limit) >= this.totalHits
            block(pageNumber, this.offset, totalPage, isLastPage)
        }
    }
}

fun PlatformSearchResult.getIds() : Set<String> {
    return when (this) {
        is CurseForgeSearchResult -> {
            data.mapTo(HashSet(data.size)) {
                it.id.toString()
            }
        }
        is ModrinthSearchResult -> {
            hits.mapTo(HashSet(hits.size)) {
                it.projectId
            }
        }
        else -> error("Unknown result type: $this")
    }
}

fun previousPage(
    pageNumber: Int,
    pages: List<AssetsPage?>,
    index: Int,
    limit: Int,
    onSuccess: (AssetsPage) -> Unit = {},
    onSearch: (index: Int) -> Unit = {}
) {
    val targetIndex = pageNumber - 2 //上一页在缓存中的索引
    val previousPage = pages.getOrNull(targetIndex)
    if (previousPage != null) {
        onSuccess(previousPage)
    } else {
        //重新搜索
        onSearch((index - limit).coerceAtLeast(0))
    }
}

fun nextPage(
    pageNumber: Int,
    isLastPage: Boolean,
    pages: List<AssetsPage?>,
    index: Int,
    limit: Int,
    onSuccess: (AssetsPage) -> Unit = {},
    onSearch: (index: Int) -> Unit = {}
) {
    if (!isLastPage) {
        val nextIndex = pageNumber
        //判断是否已缓存下一页
        val nextPage = pages.getOrNull(nextIndex)
        if (nextPage != null) {
            onSuccess(nextPage)
        } else {
            //搜索下一页
            onSearch(index + limit)
        }
    }
}
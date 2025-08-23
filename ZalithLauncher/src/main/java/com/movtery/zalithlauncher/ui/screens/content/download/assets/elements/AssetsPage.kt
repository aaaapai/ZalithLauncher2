package com.movtery.zalithlauncher.ui.screens.content.download.assets.elements

import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchData
import com.movtery.zalithlauncher.game.download.assets.utils.ModTranslations

/**
 * 资源搜索结果页面信息
 * @param pageNumber 第几页
 * @param pageIndex 页面索引
 * @param totalPage 总页数
 * @param isLastPage 是否为最后一页
 * @param data 搜索结果缓存
 */
data class AssetsPage(
    val pageNumber: Int,
    val pageIndex: Int,
    val totalPage: Int,
    val isLastPage: Boolean,
    val data: List<Pair<PlatformSearchData, ModTranslations.McMod?>>
)
package com.movtery.zalithlauncher.game.download.assets.platform

import com.movtery.zalithlauncher.R
import kotlinx.serialization.Serializable

@Serializable
enum class PlatformSortField(
    val curseforge: String,
    val modrinth: String
): PlatformFilterCode {
    /** 相关 */
    RELEVANCE("1", "relevance") {
        override fun getDisplayName(): Int = R.string.download_assets_filter_sort_by_relevant
    },

    /** 下载量 */
    DOWNLOADS("6", "downloads") {
        override fun getDisplayName(): Int = R.string.download_assets_filter_sort_by_total_downloads
    },

    /** 人气 */
    POPULARITY("2", "follows") {
        override fun getDisplayName(): Int = R.string.download_assets_filter_sort_by_popularity
    },

    /** 新创建 */
    NEWEST("11", "newest") {
        override fun getDisplayName(): Int = R.string.download_assets_filter_sort_by_recently_created
    },

    /** 最近更新 */
    UPDATED("3", "updated") {
        override fun getDisplayName(): Int = R.string.download_assets_filter_sort_by_recently_updated
    };

    override fun index(): Int = this.ordinal
}
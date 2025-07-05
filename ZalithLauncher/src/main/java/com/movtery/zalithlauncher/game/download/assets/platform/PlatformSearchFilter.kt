package com.movtery.zalithlauncher.game.download.assets.platform

data class PlatformSearchFilter(
    val searchName: String = "",
    val gameVersion: String? = null,
    val sortField: PlatformSortField = PlatformSortField.RELEVANCE,
    val category: PlatformFilterCode? = null,
    val modloader: PlatformDisplayLabel? = null,
    val index: Int = 0,
    val limit: Int = 20
)
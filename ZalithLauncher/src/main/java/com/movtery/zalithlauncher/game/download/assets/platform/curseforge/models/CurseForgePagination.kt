package com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CurseForgePagination(
    /**
     * 查询当前开始的索引
     */
    @SerialName("index")
    val index: Int,

    /**
     * 页面大小
     */
    @SerialName("pageSize")
    val pageSize: Int,

    /**
     * 查询返回的结果数
     */
    @SerialName("resultCount")
    val resultCount: Int,

    /**
     * 与查询匹配的结果总数
     */
    @SerialName("totalCount")
    val totalCount: Long
)
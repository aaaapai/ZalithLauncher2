package com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CurseForgeVersions(
    /** 响应数据 */
    @SerialName("data")
    val data: Array<CurseForgeFile>,

    /** 响应分页信息 */
    @SerialName("pagination")
    val pagination: CurseForgePagination
)
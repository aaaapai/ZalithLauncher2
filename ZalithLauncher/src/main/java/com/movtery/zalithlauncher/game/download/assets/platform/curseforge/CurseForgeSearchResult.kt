package com.movtery.zalithlauncher.game.download.assets.platform.curseforge

import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchResult
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeData
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgePagination
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CurseForgeSearchResult(
    /**
     * 响应数据
     */
    @SerialName("data")
    val data: Array<CurseForgeData>,

    /**
     * 响应分页信息
     */
    @SerialName("pagination")
    val pagination: CurseForgePagination
) : PlatformSearchResult
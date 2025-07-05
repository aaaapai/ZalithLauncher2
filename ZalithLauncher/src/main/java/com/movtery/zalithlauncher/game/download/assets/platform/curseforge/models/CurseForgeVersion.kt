package com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CurseForgeVersion(
    /** 响应数据 */
    @SerialName("data")
    val data: CurseForgeFile
)
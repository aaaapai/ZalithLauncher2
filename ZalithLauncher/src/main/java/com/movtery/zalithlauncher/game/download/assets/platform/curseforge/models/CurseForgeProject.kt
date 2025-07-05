package com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models

import com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CurseForgeProject(
    @SerialName("data")
    val data: CurseForgeData
): PlatformProject
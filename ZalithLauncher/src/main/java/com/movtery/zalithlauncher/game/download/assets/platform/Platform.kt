package com.movtery.zalithlauncher.game.download.assets.platform

import kotlinx.serialization.Serializable

/**
 * 可用的资源搜索平台
 */
@Serializable
enum class Platform(val displayName: String) {
    CURSEFORGE("CurseForge"),
    MODRINTH("Modrinth")
}
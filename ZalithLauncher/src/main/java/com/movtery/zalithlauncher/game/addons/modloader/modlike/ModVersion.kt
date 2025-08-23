package com.movtery.zalithlauncher.game.addons.modloader.modlike

import com.movtery.zalithlauncher.game.addons.modloader.AddonVersion
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthFile
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthVersion

/**
 * 模组版本，因 Modrinth 访问优势，暂只支持 Modrinth
 */
open class ModVersion(
    /** Minecraft 版本 */
    inherit: String,
    /** 显示名称 */
    val displayName: String,
    /** 版本详细信息类 */
    val version: ModrinthVersion,
    /** 可下载的主文件 */
    val file: ModrinthFile
) : AddonVersion(
    inherit = inherit
)
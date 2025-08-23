package com.movtery.zalithlauncher.game.addons.modloader.fabriclike

import com.movtery.zalithlauncher.game.addons.modloader.AddonVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersions
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersions

open class FabricLikeVersion(
    /** Minecraft 版本 */
    inherit: String,
    /** 加载器名称 */
    val loaderName: String,
    /** 加载器版本 */
    val version: String,
    /** 版本状态: true 为稳定版 (Quilt忽略此值) */
    val stable: Boolean = true
) : AddonVersion(
    inherit = inherit
) {
    val loaderUrl: String
        get() = "${getUrl(FabricVersions.officialUrl, QuiltVersions.officialUrl)}/versions/loader"

    /**
     * 获取对应版本的版本 Json 下载地址
     */
    val loaderJsonUrl: String =
        "$loaderUrl/${
            inherit.replace("∞", "infinite")
        }/$version/profile/json"


    private fun getUrl(fabric: String, quilt: String): String =
        when (this) {
            is FabricVersion -> fabric
            is QuiltVersion -> quilt
            else -> error("unknown version ${this.javaClass.simpleName}")
        }
}
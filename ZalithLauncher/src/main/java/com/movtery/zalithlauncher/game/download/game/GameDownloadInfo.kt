package com.movtery.zalithlauncher.game.download.game

import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.fabric.FabricVersion
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.quilt.QuiltVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.modlike.ModVersion
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersion

data class GameDownloadInfo(
    /** Minecraft 版本 */
    val gameVersion: String,
    /** 自定义版本名称 */
    val customVersionName: String,
    /** OptiFine 版本 */
    val optifine: OptiFineVersion? = null,
    /** Forge 版本 */
    val forge: ForgeVersion? = null,
    /** NeoForge 版本 */
    val neoforge: NeoForgeVersion? = null,
    /** Fabric 版本 */
    val fabric: FabricVersion? = null,
    /** Fabric API 版本 */
    val fabricAPI: ModVersion? = null,
    /** Quilt 版本 */
    val quilt: QuiltVersion? = null,
    /** Quilt API 版本 */
    val quiltAPI: ModVersion? = null
)

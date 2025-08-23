package com.movtery.zalithlauncher.game.download.modpack.install

import com.movtery.zalithlauncher.game.addons.modloader.ModLoader

/**
 * 整合包信息
 * @param name 整合包名称
 * @param summary 整合包的简介（可用到版本描述上）
 * @param files 整合包所有需要下载的模组
 * @param loaders 整合包需要安装的模组加载器
 * @param gameVersion 整合包需要的游戏版本
 */
data class ModPackInfo(
    val name: String,
    val summary: String? = null,
    val files: List<ModFile>,
    val loaders: List<Pair<ModLoader, String>>,
    val gameVersion: String
)

package com.movtery.zalithlauncher.game.download.modpack.platform.modrinth

/**
 * 获取 Minecraft 游戏版本
 */
fun ModrinthManifest.getGameVersion(): String = this.dependencies["minecraft"]!!

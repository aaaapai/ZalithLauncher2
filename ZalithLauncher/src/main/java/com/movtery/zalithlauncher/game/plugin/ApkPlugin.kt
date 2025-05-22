package com.movtery.zalithlauncher.game.plugin

/**
 * 启用其已识别到的软件插件
 * @param packageName 包名
 * @param appName 软件名称
 * @param appVersion 应用版本
 */
data class ApkPlugin(
    val packageName: String,
    val appName: String,
    val appVersion: String
)
package com.movtery.zalithlauncher.game.download.modpack.install

import java.io.File

/**
 * 整合包可下载模组文件
 * @param getFile 如果无法临时构建模组下载链接，或者构建模组下载链接过于耗时，则可以在这里进行构建
 */
data class ModFile(
    val outputFile: File? = null,
    val downloadUrls: List<String>? = null,
    val sha1: String? = null,
    val getFile: (suspend () -> ModFile?)? = null
)

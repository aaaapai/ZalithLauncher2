package com.movtery.zalithlauncher.game.download.game.fabric

import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.addons.mirror.mapMirrorableUrls
import com.movtery.zalithlauncher.game.addons.modloader.fabriclike.FabricLikeVersion
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

const val FABRIC_LIKE_DOWNLOAD_ID = "Download.FabricLike"

fun getFabricLikeDownloadTask(
    fabricLikeVersion: FabricLikeVersion,
    tempVersionJson: File
): Task {
    return Task.runTask(
        id = FABRIC_LIKE_DOWNLOAD_ID,
        task = {
            //下载版本 Json
            downloadJson(fabricLikeVersion, tempVersionJson)
        }
    )
}

/**
 * 下载版本 Json 文件
 */
private suspend fun downloadJson(
    fabricLikeVersion: FabricLikeVersion,
    outputFile: File
) = withContext(Dispatchers.IO) {
    val loaderJson = NetWorkUtils.fetchStringFromUrls(fabricLikeVersion.loaderJsonUrl.mapMirrorableUrls())
    outputFile
        .ensureParentDirectory()
        .writeText(loaderJson)
}
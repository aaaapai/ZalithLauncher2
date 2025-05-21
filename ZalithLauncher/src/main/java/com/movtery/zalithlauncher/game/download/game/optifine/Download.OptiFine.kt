package com.movtery.zalithlauncher.game.download.game.optifine

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersion
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersions
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import java.io.File

const val OPTIFINE_DOWNLOAD_ID = "Download.OptiFine"

fun targetTempOptiFineInstaller(tempGameDir: File, tempMinecraftDir: File, fileName: String, isNewVersion: Boolean): File {
    return if (isNewVersion) File(tempGameDir, ".temp/OptiFine.jar")
    else {
        val nameFileCleaned = fileName
            .replace("OptiFine_", "")
            .replace(".jar", "")
            .replace("preview_", "")
        val nameFileFormatted = fileName
            .replace("OptiFine_", "OptiFine-")
            .replace("preview_", "")
        File(tempMinecraftDir, "libraries/optifine/OptiFine/$nameFileCleaned/$nameFileFormatted")
    }
}

fun getOptiFineDownloadTask(
    targetTempInstaller: File,
    optifine: OptiFineVersion
): Task {
    return Task.runTask(
        id = OPTIFINE_DOWNLOAD_ID,
        task = { task ->
            task.updateProgress(-1f, R.string.download_game_install_optifine_fetch_download_url, optifine.realVersion)
            val optifineUrl = fetchOptiFineDownloadUrl(optifine)

            task.updateProgress(-1f, R.string.download_game_install_base_download_file, ModLoader.OPTIFINE.displayName, optifine.realVersion)
            NetWorkUtils.downloadFileSuspend(optifineUrl, targetTempInstaller)
        }
    )
}

fun getOptiFineModsDownloadTask(
    optifine: OptiFineVersion,
    tempModsDir: File
): Task {
    return Task.runTask(
        id = OPTIFINE_DOWNLOAD_ID,
        task = { task ->
            task.updateProgress(-1f, R.string.download_game_install_optifine_fetch_download_url, optifine.realVersion)
            val optifineUrl = fetchOptiFineDownloadUrl(optifine)

            //开始下载为 Mod
            task.updateProgress(-1f, R.string.download_game_install_base_download_file, ModLoader.OPTIFINE.displayName, optifine.realVersion)
            NetWorkUtils.downloadFileSuspend(optifineUrl, File(tempModsDir, optifine.fileName))
        }
    )
}

/**
 * 获取 OptiFine 主文件下载链接
 */
private suspend fun fetchOptiFineDownloadUrl(
    optifine: OptiFineVersion
): String = OptiFineVersions.fetchOptiFineDownloadUrl(optifine.fileName) ?: throw CantFetchingOptiFineUrlException()
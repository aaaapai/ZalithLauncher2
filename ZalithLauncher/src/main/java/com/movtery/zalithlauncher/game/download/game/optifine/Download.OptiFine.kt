package com.movtery.zalithlauncher.game.download.game.optifine

import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.addons.mirror.MirrorSource
import com.movtery.zalithlauncher.game.addons.mirror.SourceType
import com.movtery.zalithlauncher.game.addons.mirror.runMirrorable
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersion
import com.movtery.zalithlauncher.game.addons.modloader.optifine.OptiFineVersions
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.MirrorSourceType
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
            val optifineUrl = getOFUrlMirrorable(optifine)

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
            val optifineUrl = getOFUrlMirrorable(optifine)

            //开始下载为 Mod
            task.updateProgress(-1f, R.string.download_game_install_base_download_file, ModLoader.OPTIFINE.displayName, optifine.realVersion)
            NetWorkUtils.downloadFileSuspend(optifineUrl, File(tempModsDir, optifine.fileName))
        }
    )
}

private suspend fun getOFUrlMirrorable(
    optifine: OptiFineVersion
) = withContext(Dispatchers.IO) {
    val type = AllSettings.fileDownloadSource.getValue()
    runMirrorable(
        when (type) {
            MirrorSourceType.OFFICIAL_FIRST -> listOf(
                fetchOptiFineDownloadUrl(optifine, 5),
                getDownloadUrlWithBMCLAPI(optifine, 5 + 30)
            )
            MirrorSourceType.MIRROR_FIRST -> listOf(
                getDownloadUrlWithBMCLAPI(optifine, 30),
                fetchOptiFineDownloadUrl(optifine, 30 + 60)
            )
        }
    )!!
}

/**
 * 从官方源获取 OptiFine 主文件下载链接
 */
private fun fetchOptiFineDownloadUrl(
    optifine: OptiFineVersion,
    delayMillis: Long
): MirrorSource<String> = MirrorSource(
    delayMillis = delayMillis,
    type = SourceType.OFFICIAL
) {
    OptiFineVersions.fetchOptiFineDownloadUrl(optifine.fileName) ?: throw CantFetchingOptiFineUrlException()
}

/**
 * 从镜像源获取 OptiFine 主文件下载链接
 */
private fun getDownloadUrlWithBMCLAPI(
    optifine: OptiFineVersion,
    delayMillis: Long
): MirrorSource<String> = MirrorSource(
    delayMillis = delayMillis,
    type = SourceType.BMCLAPI
) {
    val inherit = if (optifine.inherit == "1.8" || optifine.inherit == "1.9") "${optifine.inherit}.0" else optifine.inherit
    val displayNameStripped = optifine.displayName.removePrefix("${optifine.inherit} ")

    val suffix = if (optifine.isPreview) {
        "HD_U_${displayNameStripped.replace(" ", "/")}"
    } else {
        "HD_U/$displayNameStripped"
    }

    "https://bmclapi2.bangbang93.com/optifine/$inherit/$suffix"
}
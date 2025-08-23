package com.movtery.zalithlauncher.game.download.game.forge

import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.game.addons.mirror.mapMirrorableUrls
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.ForgeLikeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge.ForgeVersions
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersion
import com.movtery.zalithlauncher.game.addons.modloader.forgelike.neoforge.NeoForgeVersions
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import java.io.File

const val FORGE_LIKE_DOWNLOAD_ID = "Download.ForgeLike"

fun targetTempForgeLikeInstaller(tempGameDir: File): File = File(tempGameDir, ".temp/forge_like_installer.jar")

/**
 * 判断是否为 NeoForge 版本
 */
val ForgeLikeVersion.isNeoForge: Boolean
    get() = this is NeoForgeVersion

fun getForgeLikeDownloadTask(
    targetTempInstaller: File,
    forgeLikeVersion: ForgeLikeVersion
): Task {
    return Task.runTask(
        id = FORGE_LIKE_DOWNLOAD_ID,
        task = {
            //获取安装器下载链接
            val url = if (forgeLikeVersion.isNeoForge) {
                NeoForgeVersions.getDownloadUrl(forgeLikeVersion as NeoForgeVersion)
            } else {
                ForgeVersions.getDownloadUrl(forgeLikeVersion as ForgeVersion)
            }

            NetWorkUtils.downloadFromMirrorListSuspend(url.mapMirrorableUrls(), targetTempInstaller)
        }
    )
}
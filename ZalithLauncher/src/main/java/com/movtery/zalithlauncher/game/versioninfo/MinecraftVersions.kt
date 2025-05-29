package com.movtery.zalithlauncher.game.versioninfo

import com.movtery.zalithlauncher.game.versioninfo.models.VersionManifest
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.path.UrlManager
import com.movtery.zalithlauncher.utils.GSON
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import com.movtery.zalithlauncher.utils.network.withRetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.util.concurrent.TimeUnit

object MinecraftVersions {
    private var manifest: VersionManifest? = null

    /**
     * 获取Minecraft版本信息列表
     * @param force 强制下载更新版本列表
     */
    suspend fun getVersionManifest(force: Boolean = false): VersionManifest {
        manifest?.takeIf { !force }?.let { return it }

        val localManifestFile = PathManager.FILE_MINECRAFT_VERSIONS
        val isOutdated = !localManifestFile.exists() || !localManifestFile.isFile ||
                //一天更新一次版本信息列表
                localManifestFile.lastModified() + TimeUnit.DAYS.toMillis(1) < System.currentTimeMillis()

        manifest = if (force || isOutdated) {
            downloadVersionManifest()
        } else {
            try {
                GSON.fromJson(localManifestFile.readText(), VersionManifest::class.java)
            } catch (e: Exception) {
                lWarning("Failed to parse version manifest, will redownload", e)
                //读取失败则删除当前的版本信息文件
                FileUtils.deleteQuietly(localManifestFile)
                downloadVersionManifest()
            }
        }

        return manifest ?: throw IllegalStateException("Version manifest is null after all attempts")
    }

    /**
     * 从官方版本仓库获取版本信息
     */
    private suspend fun downloadVersionManifest(): VersionManifest {
        return withContext(Dispatchers.IO) {
            withRetry("MinecraftVersions", maxRetries = 1) {
                val rawJson = NetWorkUtils.fetchStringFromUrl(UrlManager.URL_MINECRAFT_VERSION_REPOS)
                val versionManifest = GSON.fromJson(rawJson, VersionManifest::class.java)
                PathManager.FILE_MINECRAFT_VERSIONS.writeText(rawJson)
                versionManifest
            }
        }
    }
}
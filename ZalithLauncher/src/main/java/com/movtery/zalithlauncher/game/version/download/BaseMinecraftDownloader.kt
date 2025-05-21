package com.movtery.zalithlauncher.game.version.download

import com.movtery.zalithlauncher.game.path.getAssetsHome
import com.movtery.zalithlauncher.game.path.getLibrariesHome
import com.movtery.zalithlauncher.game.path.getResourcesHome
import com.movtery.zalithlauncher.game.path.getVersionsHome
import com.movtery.zalithlauncher.game.versioninfo.MinecraftVersions
import com.movtery.zalithlauncher.game.versioninfo.models.AssetIndexJson
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest
import com.movtery.zalithlauncher.game.versioninfo.models.VersionManifest.Version
import com.movtery.zalithlauncher.utils.classes.Quadruple
import com.movtery.zalithlauncher.utils.file.ensureDirectory
import com.movtery.zalithlauncher.utils.file.ensureParentDirectory
import java.io.File

const val DOWNLOADER_TAG = "MinecraftDownloader"
const val MINECRAFT_RES: String = "https://resources.download.minecraft.net/"

/**
 * 设计为通用化 Minecraft 原本完整下载
 */
class BaseMinecraftDownloader(
    private val verifyIntegrity: Boolean
) {
    //Dir
    val assetsTarget = File(getAssetsHome()).ensureDirectory()
    val resourcesTarget = File(getResourcesHome()).ensureDirectory()
    val versionsTarget = File(getVersionsHome()).ensureDirectory()
    val librariesTarget = File(getLibrariesHome()).ensureDirectory()
    val assetIndexTarget = File(assetsTarget, "indexes").ensureDirectory()

    suspend fun findVersion(version: String): Version? {
        val versionManifest = MinecraftVersions.getVersionManifest()
        return versionManifest.versions.find { it.id == version }
    }

    fun getVersionJsonPath(version: String, mcFolder: File = versionsTarget) =
        File(mcFolder, "$version/$version.json".replace("/", File.separator)).ensureParentDirectory()

    fun getVersionJarPath(version: String, mcFolder: File = versionsTarget) =
        File(mcFolder, "$version/$version.jar".replace("/", File.separator)).ensureParentDirectory()

    /**
     * 创建版本 Json
     */
    suspend fun createVersionJson(version: Version): GameManifest {
        return createVersionJson(version, version.id)
    }

    /**
     * 创建版本 Json
     * @param targetVersion 目标版本名称
     */
    suspend fun createVersionJson(
        version: Version,
        targetVersion: String,
        mcFolder: File = versionsTarget
    ): GameManifest {
        return downloadAndParseJson(
            targetFile = getVersionJsonPath(targetVersion, mcFolder),
            url = version.url,
            expectedSHA = version.sha1,
            verifyIntegrity = verifyIntegrity,
            classOfT = GameManifest::class.java
        )
    }

    /**
     * 创建 assets 索引 Json
     */
    suspend fun createAssetIndex(
        assetIndexTarget: File,
        gameManifest: GameManifest
    ): AssetIndexJson? {
        val indexFile = File(assetIndexTarget, "${gameManifest.assets}.json")
        return gameManifest.assetIndex?.let { assetIndex ->
            downloadAndParseJson(
                targetFile = indexFile,
                url = assetIndex.url,
                expectedSHA = assetIndex.sha1,
                verifyIntegrity = verifyIntegrity,
                classOfT = AssetIndexJson::class.java
            )
        }
    }

    /** 计划客户端jar下载 */
    fun loadClientJarDownload(
        gameManifest: GameManifest,
        clientName: String,
        mcFolder: File = versionsTarget,
        scheduleDownload: (url: String, hash: String?, targetFile: File, size: Long) -> Unit
    ) {
        val clientFile = getVersionJarPath(clientName, mcFolder)
        gameManifest.downloads?.client?.let { client ->
            scheduleDownload(client.url, client.sha1, clientFile, client.size)
        }
    }

    /** 计划assets资产下载 */
    fun loadAssetsDownload(
        assetIndex: AssetIndexJson?,
        resourcesTargetDir: File = resourcesTarget,
        assetsTargetDir: File = assetsTarget,
        scheduleDownload: (url: String, hash: String?, targetFile: File, size: Long) -> Unit
    ) {
        assetIndex?.objects?.forEach { (path, objectInfo) ->
            val hashedPath = "${objectInfo.hash.substring(0, 2)}/${objectInfo.hash}"
            val targetPath = if (assetIndex.isMapToResources) resourcesTargetDir else assetsTargetDir
            val targetFile: File = if (assetIndex.isVirtual || assetIndex.isMapToResources) {
                File(targetPath, path)
            } else {
                File(targetPath, "objects/${hashedPath}".replace("/", File.separator))
            }
            scheduleDownload("$MINECRAFT_RES$hashedPath", objectInfo.hash, targetFile, objectInfo.size)
        }
    }

    /** 计划库文件下载 */
    fun loadLibraryDownloads(
        gameManifest: GameManifest,
        targetDir: File = librariesTarget,
        scheduleDownload: (url: String, hash: String?, targetFile: File, size: Long, isDownloadable: Boolean) -> Unit
    ) {
        gameManifest.libraries?.let { libraries ->
            processLibraries { libraries }
            libraries.forEach { library ->
                if (library.name.startsWith("org.lwjgl")) return@forEach

                val artifactPath: String = artifactToPath(library) ?: return@forEach
                val (sha1, url, size, isDownloadable) = library.downloads?.let { downloads ->
                    downloads.artifact?.let { artifact ->
                        Quadruple(artifact.sha1, artifact.url, artifact.size, true)
                    } ?: return@forEach
                } ?: run {
                    var isDownloadable = true
                    val u1 = library.url?.replace("http://", "https://") ?: run {
                        //对于没有提供下载链接的，可能是需要文件已经安装，而不是临时获取
                        //不过尝试使用官方源下载，若下载失败则表明这个版本 文件有缺失的情况
                        isDownloadable = false
                        "https://libraries.minecraft.net/"
                    }
                    val url = u1.let { "${it}$artifactPath" }
                    Quadruple(library.sha1, url, library.size, isDownloadable)
                }

                scheduleDownload(url, sha1, File(targetDir, artifactPath), size, isDownloadable)
            }
        }
    }

//    /** 计划日志格式化配置下载 */
//    fun loadLog4jXMLDownload(
//        gameManifest: GameManifest,
//        version: String,
//        scheduleDownload: (url: String, hash: String?, targetFile: File, size: Long) -> Unit
//    ) {
//        val versionLoggingTarget = getLog4jXMLPath(version)
//        gameManifest.logging?.client?.file?.let { loggingConfig ->
//            scheduleDownload(loggingConfig.url, loggingConfig.sha1, versionLoggingTarget, loggingConfig.size)
//        }
//    }
}
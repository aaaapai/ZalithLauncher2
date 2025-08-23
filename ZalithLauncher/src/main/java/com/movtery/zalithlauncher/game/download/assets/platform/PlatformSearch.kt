package com.movtery.zalithlauncher.game.download.assets.platform

import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.CurseForgeSearchRequest
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.CurseForgeSearchResult
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeFile
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeFingerprintsMatches
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeProject
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeVersion
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeVersions
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.ModrinthSearchRequest
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.ModrinthSearchResult
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthSingleProject
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthVersion
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.utils.file.MurmurHash2Incremental
import com.movtery.zalithlauncher.utils.network.httpGet
import com.movtery.zalithlauncher.utils.network.httpPostJson
import com.movtery.zalithlauncher.utils.network.withRetry
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.Parameters
import java.io.File

object PlatformSearch {
    /**
     * CurseForge 平台的 API 链接
     * [CurseForge REST API](https://docs.curseforge.com/rest-api/?shell#base-url)
     */
    const val CURSEFORGE_API = "https://api.curseforge.com/v1"

    /**
     * Modrinth 平台的 API 链接
     * [Modrinth Docs](https://docs.modrinth.com/api/operations/searchprojects)
     */
    const val MODRINTH_API = "https://api.modrinth.com/v2"

    /**
     * 向 CurseForge 平台发送搜索请求
     * @param request 搜索请求
     * @param apiKey CurseForge API 密钥
     */
    suspend fun searchWithCurseforge(
        request: CurseForgeSearchRequest,
        apiKey: String = InfoDistributor.CURSEFORGE_API,
        retry: Int = 3
    ): CurseForgeSearchResult = withRetry("PlatformSearch:CurseForge_search", maxRetries = retry) {
        httpGet(
            url = "$CURSEFORGE_API/mods/search",
            headers = listOf("x-api-key" to apiKey),
            parameters = request.toParameters()
        )
    }

    /**
     * 在 CurseForge 平台获取项目详细信息
     * @param apiKey CurseForge API 密钥
     */
    suspend fun getProjectFromCurseForge(
        projectID: String,
        apiKey: String = InfoDistributor.CURSEFORGE_API,
        retry: Int = 3
    ): CurseForgeProject = withRetry("PlatformSearch:CurseForge_getProject", maxRetries = retry) {
        httpGet(
            url = "$CURSEFORGE_API/mods/$projectID",
            headers = listOf("x-api-key" to apiKey)
        )
    }

    /**
     * 在 CurseForge 平台根据分页获取项目的版本列表
     * @param apiKey CurseForge API 密钥
     * @param index 开始处
     * @param pageSize 每页请求数量
     */
    suspend fun getVersionsFromCurseForge(
        projectID: String,
        apiKey: String = InfoDistributor.CURSEFORGE_API,
        index: Int = 0,
        pageSize: Int = 100,
        retry: Int = 3
    ): CurseForgeVersions = withRetry("PlatformSearch:CurseForge_getVersions", maxRetries = retry) {
        httpGet(
            url = "$CURSEFORGE_API/mods/$projectID/files",
            headers = listOf("x-api-key" to apiKey),
            parameters = Parameters.build {
                append("index", index.toString())
                append("pageSize", pageSize.toString())
            }
        )
    }

    /**
     * 持续分页获取 CurseForge 项目的所有版本文件，直到全部加载完成
     * @param projectID 项目ID
     * @param apiKey CurseForge API 密钥
     * @param pageSize 每页请求数量
     */
    suspend fun getAllVersionsFromCurseForge(
        projectID: String,
        apiKey: String = InfoDistributor.CURSEFORGE_API,
        pageSize: Int = 100,
        retry: Int = 3
    ): List<CurseForgeFile> {
        val allFiles = mutableListOf<CurseForgeFile>()
        var index = 0

        while (true) {
            val response: CurseForgeVersions = getVersionsFromCurseForge(
                projectID = projectID,
                apiKey = apiKey,
                index = index,
                pageSize = pageSize,
                retry
            )
            val files = response.data
            allFiles.addAll(files)

            //少于pageSize，已经是最后一页
            if (files.size < pageSize) break

            index += pageSize
        }

        return allFiles
    }

    /**
     * 在 CurseForge 平台获取某项目的某个文件
     */
    suspend fun getVersionFromCurseForge(
        projectID: String,
        fileID: String,
        apiKey: String = InfoDistributor.CURSEFORGE_API,
        retry: Int = 3
    ): CurseForgeVersion = withRetry("PlatformSearch:CurseForge_getVersion", maxRetries = retry) {
        httpGet(
            url = "$CURSEFORGE_API/mods/$projectID/files/$fileID",
            headers = listOf("x-api-key" to apiKey)
        )
    }

    suspend fun getVersionByLocalFileFromCurseForge(
        file: File,
        apiKey: String = InfoDistributor.CURSEFORGE_API,
        retry: Int = 1
    ): CurseForgeFingerprintsMatches = withRetry("PlatformSearch:CurseForge_getVersionByLocalFile", maxRetries = retry) {
        val hash = MurmurHash2Incremental.computeHash(file, byteToSkip = listOf(0x9, 0xa, 0xd, 0x20))
        httpPostJson(
            url = "$CURSEFORGE_API/fingerprints",
            headers = listOf("x-api-key" to apiKey),
            body = mapOf("fingerprints" to listOf(hash))
        )
    }

    /**
     * 向 Modrinth 平台发送搜索请求
     * @param request 搜索请求
     */
    suspend fun searchWithModrinth(
        request: ModrinthSearchRequest,
        retry: Int = 3
    ): ModrinthSearchResult = withRetry("PlatformSearch:Modrinth_search", maxRetries = retry) {
        httpGet(
            url = "$MODRINTH_API/search",
            parameters = request.toParameters()
        )
    }

    /**
     * 在 Modrinth 平台获取项目详细信息
     */
    suspend fun getProjectFromModrinth(
        projectID: String,
        retry: Int = 3
    ): ModrinthSingleProject = withRetry("PlatformSearch:Modrinth_getProject", maxRetries = retry) {
        httpGet(
            url = "$MODRINTH_API/project/$projectID"
        )
    }

    /**
     * 获取 Modrinth 项目的所有版本
     */
    suspend fun getVersionsFromModrinth(
        projectID: String,
        retry: Int = 3
    ): List<ModrinthVersion> = withRetry("PlatformSearch:Modrinth_getVersions", maxRetries = retry) {
        httpGet(
            url = "$MODRINTH_API/project/$projectID/version"
        )
    }

    suspend fun getVersionByLocalFileFromModrinth(
        sha1: String,
        retry: Int = 1
    ): ModrinthVersion? = withRetry("PlatformSearch:Modrinth_getVersionByLocalFile", maxRetries = retry) {
        try {
            httpGet(
                url = "$MODRINTH_API/version_file/$sha1",
                parameters = Parameters.build {
                    append("algorithm", "sha1")
                }
            )
        } catch (_: ClientRequestException) {
            return@withRetry null
        }
    }
}
package com.movtery.zalithlauncher.game.download.assets.platform

import com.movtery.zalithlauncher.game.download.assets.mapExceptionToMessage
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearch.searchWithCurseforge
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearch.searchWithModrinth
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.CurseForgeSearchRequest
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeCategory
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeFile
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.ModrinthSearchRequest
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthFacet
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthModLoaderCategory
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthVersion
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.VersionFacet
import com.movtery.zalithlauncher.game.download.assets.utils.localizedModSearchKeywords
import com.movtery.zalithlauncher.game.download.assets.utils.processChineseSearchResults
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.DownloadAssetsState
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.SearchAssetsState
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.io.File

suspend fun searchAssets(
    searchPlatform: Platform,
    searchFilter: PlatformSearchFilter,
    platformClasses: PlatformClasses,
    onSuccess: suspend (PlatformSearchResult) -> Unit,
    onError: (SearchAssetsState.Error) -> Unit
) {
    runCatching {
        val (containsChinese, englishKeywords) = searchFilter.searchName.localizedModSearchKeywords(platformClasses)
        val query = englishKeywords?.joinToString(" ") ?: searchFilter.searchName
        val result = when (searchPlatform) {
            Platform.CURSEFORGE -> {
                searchWithCurseforge(
                    request = CurseForgeSearchRequest(
                        classId = platformClasses.curseforge.classID,
                        categories = setOfNotNull(
                            searchFilter.category?.let { category ->
                                category as? CurseForgeCategory
                            }
                        ),
                        searchFilter = query,
                        gameVersion = searchFilter.gameVersion,
                        sortField = searchFilter.sortField,
                        modLoader = searchFilter.modloader as? CurseForgeModLoader,
                        index = searchFilter.index,
                        pageSize = searchFilter.limit
                    ),
                    retry = 1 //只尝试一次
                )
            }
            Platform.MODRINTH -> {
                searchWithModrinth(
                    request = ModrinthSearchRequest(
                        query = query,
                        facets = listOfNotNull(
                            platformClasses.modrinth!!, //必须为非空处理
                            searchFilter.gameVersion?.let { version ->
                                VersionFacet(version)
                            },
                            searchFilter.category?.let { category ->
                                category as? ModrinthFacet
                            },
                            searchFilter.modloader?.let { modloader ->
                                modloader as? ModrinthModLoaderCategory
                            }
                        ),
                        index = searchFilter.sortField,
                        offset = searchFilter.index,
                        limit = searchFilter.limit
                    ),
                    retry = 1 //只尝试一次
                )
            }
        }
        onSuccess(
            if (containsChinese) result.processChineseSearchResults(searchFilter.searchName, platformClasses)
            else result
        )
    }.onFailure { e ->
        if (e !is CancellationException) {
            lError("An exception occurred while searching for assets.", e)
            val pair = mapExceptionToMessage(e)
            val state = SearchAssetsState.Error(pair.first, pair.second)
            onError(state)
        } else {
            lWarning("The search task has been cancelled.")
        }
    }
}

suspend fun <E> getVersions(
    projectID: String,
    platform: Platform,
    onSuccess: suspend (List<PlatformVersion>) -> Unit,
    onError: (DownloadAssetsState<List<E>>) -> Unit
) {
    runCatching {
        val result = when (platform) {
            Platform.CURSEFORGE -> PlatformSearch.getAllVersionsFromCurseForge(projectID)
            Platform.MODRINTH -> PlatformSearch.getVersionsFromModrinth(projectID)
        }
        onSuccess(result)
    }.onFailure { e ->
        if (e !is CancellationException) {
            lError("An exception occurred while retrieving the project version.", e)
            val pair = mapExceptionToMessage(e)
            val state = DownloadAssetsState.Error<List<E>>(pair.first, pair.second)
            onError(state)
        } else {
            lWarning("The version retrieval task has been cancelled.")
        }
    }
}

suspend fun <E> getProject(
    projectID: String,
    platform: Platform,
    onSuccess: (PlatformProject) -> Unit,
    onError: (DownloadAssetsState<E>, Throwable) -> Unit
) {
    runCatching {
        when (platform) {
            Platform.CURSEFORGE -> PlatformSearch.getProjectFromCurseForge(projectID)
            Platform.MODRINTH -> PlatformSearch.getProjectFromModrinth(projectID)
        }
    }.fold(
        onSuccess = onSuccess,
        onFailure = { e ->
            if (e !is CancellationException) {
                lError("An exception occurred while retrieving project information.", e)
                val pair = mapExceptionToMessage(e)
                val state = DownloadAssetsState.Error<E>(pair.first, pair.second)
                onError(state, e)
            } else {
                lWarning("The project retrieval task has been cancelled.")
            }
        }
    )
}

suspend fun getProjectByVersion(
    version: PlatformVersion
): PlatformProject = withContext(Dispatchers.IO) {
    when (version) {
        is ModrinthVersion -> PlatformSearch.getProjectFromModrinth(projectID = version.projectId)
        is CurseForgeFile -> PlatformSearch.getProjectFromCurseForge(projectID = version.modId.toString())
        else -> error("Unknown version type: $version")
    }
}

suspend fun getVersionByLocalFile(file: File, sha1: String): PlatformVersion? = coroutineScope {
    val modrinthDeferred = async(Dispatchers.IO) {
        runCatching {
            PlatformSearch.getVersionByLocalFileFromModrinth(sha1)
        }.getOrNull()
    }

    val curseForgeDeferred = async(Dispatchers.IO) {
        runCatching {
            PlatformSearch.getVersionByLocalFileFromCurseForge(file)
                .data.exactMatches
                ?.takeIf { it.isNotEmpty() }
                ?.firstOrNull()
                ?.file
        }.getOrNull()
    }

    val result = select {
        modrinthDeferred.onAwait { result ->
            if (result != null) {
                curseForgeDeferred.cancel()
                result
            } else {
                null
            }
        }
        curseForgeDeferred.onAwait { result ->
            if (result != null) {
                modrinthDeferred.cancel()
                result
            } else {
                null
            }
        }
    }

    result ?: run {
        if (!modrinthDeferred.isCompleted) modrinthDeferred.await()
        else if (!curseForgeDeferred.isCompleted) curseForgeDeferred.await()
        else null
    }
}
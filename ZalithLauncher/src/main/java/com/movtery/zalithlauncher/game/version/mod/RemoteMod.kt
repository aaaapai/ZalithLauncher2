package com.movtery.zalithlauncher.game.version.mod

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeFile
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.getProjectByVersion
import com.movtery.zalithlauncher.game.download.assets.platform.getVersionByLocalFile
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthModLoaderCategory
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthVersion
import com.movtery.zalithlauncher.game.download.assets.utils.ModTranslations
import com.movtery.zalithlauncher.game.download.assets.utils.getMcMod
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.toInfo
import com.movtery.zalithlauncher.utils.file.calculateFileSha1
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class RemoteMod(
    val localMod: LocalMod
) {
    /**
     * 是否正在加载项目信息
     */
    var isLoading by mutableStateOf(false)
        private set

    /**
     * 远端设置的加载器列表
     */
    var remoteLoaders: ModVersionLoaders? by mutableStateOf(null)
        private set

    /**
     * 项目信息
     */
    var projectInfo: ModProject? by mutableStateOf(null)
        private set

    /**
     * 项目翻译信息
     */
    var mcMod: ModTranslations.McMod? by mutableStateOf(null)
        private set

    /**
     * 是否已经加载过
     */
    var isLoaded: Boolean = false
        private set

    /**
     * @param loadFromCache 是否从缓存中加载
     */
    suspend fun load(loadFromCache: Boolean) {
        if (loadFromCache && isLoaded) return

        if (!loadFromCache) {
            remoteLoaders = null
            projectInfo = null
            mcMod = null
        }

        isLoading = true

        try {
            withContext(Dispatchers.IO) {
                val file = localMod.file
                val modProjectCache = modProjectCache()
                val modVersionCache = modVersionCache()

                runCatching {
                    //获取文件 sha1，作为缓存的键
                    val sha1 = calculateFileSha1(file)

                    if (loadFromCache && modVersionCache.containsKey(sha1)) {
                        modVersionCache.decodeParcelable(sha1, ModVersionLoaders::class.java)?.let { loaders ->
                            remoteLoaders = loaders
                        }
                    }

                    if (loadFromCache && modProjectCache.containsKey(sha1)) {
                        modProjectCache.decodeParcelable(sha1, ModProject::class.java)?.let { project ->
                            mcMod = ModTranslations.getTranslationsByRepositoryType(PlatformClasses.MOD).getModBySlugId(project.slug)
                            return@runCatching project.also {
                                isLoaded = true
                            }
                        }
                    }

                    val version = getVersionByLocalFile(file, sha1)
                    ensureActive()

                    version?.let { ver ->
                        updateLoaders(sha1, ver, modVersionCache)
                        getProjectByVersion(ver).let { project ->
                            mcMod = project.getMcMod(PlatformClasses.MOD)
                            project.toInfo(PlatformClasses.MOD).let { info ->
                                ModProject(
                                    id = info.id,
                                    platform = info.platform,
                                    iconUrl = info.iconUrl,
                                    title = info.title,
                                    slug = info.slug
                                )
                            }.also { project ->
                                modProjectCache.encode(sha1, project, MMKV.ExpireInDay)
                            }
                        }
                    }
                }.onSuccess { info ->
                    projectInfo = info.also {
                        isLoaded = true
                    }
                }.onFailure { e ->
                    if (e is CancellationException) return@onFailure
                    lWarning("Failed to load project info for mod: ${file.name}", e)
                }
            }
        } finally {
            isLoading = false
        }
    }

    private fun updateLoaders(
        fileSHA1: String,
        version: PlatformVersion,
        modVersionCache: MMKV,
    ) {
        remoteLoaders = null
        remoteLoaders = when (version) {
            is ModrinthVersion -> {
                ModVersionLoaders(
                    version.loaders.mapNotNull { loaderName ->
                        ModrinthModLoaderCategory.entries.find { it.facetValue() == loaderName }
                    }.toTypedArray()
                )
            }
            is CurseForgeFile -> {
                ModVersionLoaders(
                    version.gameVersions.mapNotNull { loaderName ->
                        CurseForgeModLoader.entries.find {
                            it.getDisplayName().equals(loaderName, true)
                        }
                    }.toTypedArray()
                )
            }
            else -> error("Unknown version type: $version")
        }.also { loaders ->
            modVersionCache.encode(fileSHA1, loaders, MMKV.ExpireInDay)
        }
    }
}
package com.movtery.zalithlauncher.game.download.assets.utils

import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchResult
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.CurseForgeSearchResult
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeProject
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.ModrinthSearchResult
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthSingleProject
import com.movtery.zalithlauncher.utils.isChinese
import com.movtery.zalithlauncher.utils.string.StringUtils
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.containsChinese
import kotlin.math.max

private const val CONTAIN_CHINESE_WEIGHT = 10

/**
 * 根据平台获取模组翻译信息
 */
fun PlatformProject.getMcMod(
    classes: PlatformClasses
): ModTranslations.McMod? {
    val translations = ModTranslations.getTranslationsByRepositoryType(classes)
    return when (this) {
        is ModrinthSingleProject -> translations.getModBySlugId(slug)
        is CurseForgeProject -> translations.getModBySlugId(data.slug)
        else -> error("Unknown project type: $this")
    }
}

/**
 * 获取 mcmod 模组翻译标题，若当前环境非中文环境，则返回原始模组名称
 */
fun ModTranslations.McMod?.getMcmodTitle(originTitle: String): String {
    return this?.displayName?.takeIf { isChinese() } ?: originTitle
}

/**
 * 修改自源代码：[HMCL Github](https://github.com/HMCL-dev/HMCL/blob/57018be/HMCL/src/main/java/org/jackhuang/hmcl/game/LocalizedRemoteModRepository.java#L45-L63)
 * 原项目版权归原作者所有，遵循GPL v3协议
 * @return `Boolean` 是否包含中文, `String` 英文混合关键词 (不包含中文时，原样返回)
 */
fun String.localizedModSearchKeywords(
    classes: PlatformClasses
): Pair<Boolean, Set<String>?> {
    if (!this.containsChinese()) return false to null
    val englishSearchFiltersSet: MutableSet<String> = HashSet(16)

    for ((count, mod) in ModTranslations.getTranslationsByRepositoryType(classes)
        .searchMod(this).withIndex()
    ) {
        for (englishWord in StringUtils.tokenize(mod.subname.ifBlank { mod.name })) {
            if (englishSearchFiltersSet.contains(englishWord)) continue
            englishSearchFiltersSet.add(englishWord)
        }
        if (count >= 3) break
    }

    return true to englishSearchFiltersSet
}

/**
 * 参考源代码：[HMCL Github](https://github.com/HMCL-dev/HMCL/blob/57018be/HMCL/src/main/java/org/jackhuang/hmcl/game/LocalizedRemoteModRepository.java#L65-L103)
 * 原项目版权归原作者所有，遵循GPL v3协议
 * @return 对于中文搜索结果的优先级排序
 */
fun PlatformSearchResult.processChineseSearchResults(
    searchFilter: String,
    classes: PlatformClasses
): PlatformSearchResult {
    fun <T> List<T>.processList(getSlug: (T) -> String?): List<T> {
        val (chineseResults, englishResults) = partition { mod ->
            ModTranslations.getTranslationsByRepositoryType(classes)
                .getModBySlugId(getSlug(mod))
                ?.name
                ?.takeIf { it.isNotBlank() && it.containsChinese() } != null
        }

        val levCalculator = StringUtils.LevCalculator()
        val sortedChineseResults = chineseResults.map { mod ->
            val translation = ModTranslations.getTranslationsByRepositoryType(classes)
                .getModBySlugId(getSlug(mod))!!
            val modName = translation.name

            val relevanceScore = when {
                searchFilter.isEmpty() || modName.isEmpty() ->
                    max(searchFilter.length, modName.length)
                else -> {
                    var levDistance = levCalculator.calc(searchFilter, modName)
                    searchFilter.forEach { char ->
                        if (modName.contains(char)) levDistance -= CONTAIN_CHINESE_WEIGHT
                    }
                    levDistance
                }
            }
            mod to relevanceScore
        }.sortedBy { it.second }
            .map { it.first }

        return sortedChineseResults + englishResults
    }

    return when (this) {
        is CurseForgeSearchResult -> {
            val newData = data.toList()
                .processList { it.slug }
                .toTypedArray()
            CurseForgeSearchResult(newData, pagination)
        }
        is ModrinthSearchResult -> {
            val newHits = hits.toList()
                .processList { it.slug }
                .toTypedArray()
            ModrinthSearchResult(newHits, offset, limit, totalHits)
        }
        else -> this
    }
}
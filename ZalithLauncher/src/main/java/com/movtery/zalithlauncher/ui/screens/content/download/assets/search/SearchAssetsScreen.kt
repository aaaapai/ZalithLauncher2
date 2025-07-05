package com.movtery.zalithlauncher.ui.screens.content.download.assets.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformFilterCode
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearch
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchFilter
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchResult
import com.movtery.zalithlauncher.game.download.assets.platform.getIds
import com.movtery.zalithlauncher.game.download.assets.platform.getPageInfo
import com.movtery.zalithlauncher.game.download.assets.platform.mcmod.models.McModSearchRes
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthModLoaderCategory
import com.movtery.zalithlauncher.game.download.assets.platform.nextPage
import com.movtery.zalithlauncher.game.download.assets.platform.previousPage
import com.movtery.zalithlauncher.game.download.assets.platform.searchAssets
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.content.DownloadScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.AssetsPage
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.ResultListLayout
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.SearchAssetsState
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.SearchFilter
import com.movtery.zalithlauncher.ui.screens.main.elements.mainScreenKey
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.logging.Logger.lInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 资源搜索屏幕的 view model
 * @param initialPlatform 初始设定的平台
 * @param platformClasses 资源搜索的类型
 */
private class ScreenViewModel(
    initialPlatform: Platform,
    private val platformClasses: PlatformClasses
): ViewModel() {
    var searchResult by mutableStateOf<SearchAssetsState>(SearchAssetsState.Searching)
    val pages = mutableStateListOf<AssetsPage?>()

    var searchPlatform by mutableStateOf(initialPlatform)
    var searchFilter by mutableStateOf(PlatformSearchFilter())

    var currentSearchJob: Job? = null

    /**
     * 更新过滤器时，重置已有结果，重新触发搜索
     */
    fun researchWithFilter(filter: PlatformSearchFilter) {
        pages.clear()
        searchFilter = filter.copy(index = 0) //重置索引到起始处
        search()
    }

    fun putRes(result: PlatformSearchResult, mcmod:McModSearchRes? = null) {
        result.getPageInfo { pageNumber, pageIndex, totalPage, isLastPage ->
            lInfo("Searched page info: {pageNumber: $pageNumber, pageIndex: $pageIndex, totalPage: $totalPage, isLastPage: $isLastPage}")

            val page = AssetsPage(
                pageNumber = pageNumber,
                pageIndex = pageIndex,
                totalPage = totalPage,
                isLastPage = isLastPage,
                result = result,
                mcmod = mcmod
            )

            val targetIndex = pageNumber - 1

            if (pages.size > targetIndex) {
                pages[targetIndex] = page //替换已有页
            } else {
                while (pages.size < targetIndex) {
                    pages += null
                }
                pages += page
            }

            searchResult = SearchAssetsState.Success(page)
        }
    }

    fun search() {
        currentSearchJob?.cancel() //取消上一个搜索

        currentSearchJob = viewModelScope.launch {
            searchResult = SearchAssetsState.Searching
            searchAssets(
                searchPlatform = searchPlatform,
                searchFilter = searchFilter,
                platformClasses = platformClasses,
                onSuccess = { result ->
                    val type: Int = when (searchPlatform) {
                        Platform.MODRINTH -> 1
                        else -> 0
                    }
                    val mctype: Int = when (platformClasses) {
                        PlatformClasses.MOD -> 0
                        PlatformClasses.MOD_PACK -> 1
                        else -> -1
                    }

                    val locale: Locale = Locale.getDefault()

                    if (locale.language.equals("zh") && locale.country.equals("CN")) {
                        currentSearchJob = viewModelScope.launch {
                            val res = PlatformSearch.getMcmodModInfo(type, result.getIds(), mctype)
                            putRes(result, res)
                        }
                    } else {
                        putRes(result)
                    }
                },
                onError = {
                    searchResult = it
                }
            )
        }
    }

    init {
        //初始化后，执行一次搜索
        search()
    }

    override fun onCleared() {
        currentSearchJob?.cancel()
    }
}

@Composable
private fun rememberSearchAssetsViewModel(
    navKey: NavKey,
    initialPlatform: Platform,
    platformClasses: PlatformClasses
): ScreenViewModel {
    return viewModel(
        key = navKey.toString()
    ) {
        ScreenViewModel(initialPlatform, platformClasses)
    }
}

/**
 * @param parentScreenKey 父屏幕Key
 * @param parentCurrentKey 父屏幕当前Key
 * @param screenKey 屏幕的Key
 * @param currentKey 当前的Key
 * @param platformClasses 搜索资源的分类
 * @param initialPlatform 初始搜索平台
 * @param enablePlatform 是否允许更改平台
 * @param getCategories 根据平台获取可用的资源类别过滤器
 * @param enableModLoader 是否允许更改模组加载器
 * @param getModloaders 根据平台获取可用的模组加载器过滤器
 * @param mapCategories 通过平台获取类别本地化信息
 * @param swapToDownload 跳转到下载详情页
 */
@Composable
fun SearchAssetsScreen(
    parentScreenKey: NavKey,
    parentCurrentKey: NavKey?,
    screenKey: NavKey,
    currentKey: NavKey?,
    platformClasses: PlatformClasses,
    initialPlatform: Platform,
    enablePlatform: Boolean = true,
    getCategories: (Platform) -> List<PlatformFilterCode>,
    enableModLoader: Boolean = false,
    getModloaders: (Platform) -> List<PlatformDisplayLabel> = { emptyList() },
    mapCategories: (Platform, String) -> PlatformFilterCode?,
    swapToDownload: (Platform, projectId: String) -> Unit = { _, _ -> }
) {
    val viewModel: ScreenViewModel = rememberSearchAssetsViewModel(
        navKey = screenKey,
        initialPlatform = initialPlatform,
        platformClasses = platformClasses
    )

    //跟随平台自动变更的内容
    val categories = remember(viewModel.searchPlatform) {
        getCategories(viewModel.searchPlatform)
    }
    val modloaders = remember(viewModel.searchPlatform) {
        getModloaders(viewModel.searchPlatform)
    }

    BaseScreen(
        levels1 = listOf(
            Pair(DownloadScreenKey::class.java, mainScreenKey)
        ),
        Triple(parentScreenKey, parentCurrentKey, false),
        Triple(screenKey, currentKey, false)
    ) { isVisible ->
        Row {
            val yOffset by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible)
            ResultListLayout(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(7f)
                    .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                searchState = viewModel.searchResult,
                onReload = {
                    viewModel.search()
                },
                mapCategories = mapCategories,
                mapModLoaders = { string ->
                    ModrinthModLoaderCategory.entries.find { it.facetValue() == string }
                },
                swapToDownload = swapToDownload,
                onPreviousPage = { pageNumber ->
                    previousPage(
                        pageNumber = pageNumber,
                        pages = viewModel.pages,
                        index = viewModel.searchFilter.index,
                        limit = viewModel.searchFilter.limit,
                        onSuccess = { previousPage ->
                            viewModel.searchResult = SearchAssetsState.Success(previousPage)
                        },
                        onSearch = { newIndex ->
                            viewModel.searchFilter = viewModel.searchFilter.copy(index = newIndex)
                            viewModel.search() //搜索上一页
                        }
                    )
                },
                onNextPage = { pageNumber, isLastPage ->
                    nextPage(
                        pageNumber = pageNumber,
                        isLastPage = isLastPage,
                        pages = viewModel.pages,
                        index = viewModel.searchFilter.index,
                        limit = viewModel.searchFilter.limit,
                        onSuccess = { nextPage ->
                            viewModel.searchResult = SearchAssetsState.Success(nextPage)
                        },
                        onSearch = { newIndex ->
                            viewModel.searchFilter = viewModel.searchFilter.copy(index = newIndex)
                            viewModel.search() //搜索下一页
                        }
                    )
                }
            )

            val xOffset by swapAnimateDpAsState(
                targetValue = 40.dp,
                swapIn = isVisible,
                isHorizontal = true
            )
            SearchFilter(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(3f)
                    .offset { IntOffset(x = xOffset.roundToPx(), y = 0) },
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp, end = 12.dp),
                enablePlatform = enablePlatform,
                searchPlatform = viewModel.searchPlatform,
                onPlatformChange = {
                    viewModel.searchPlatform = it
                    viewModel.researchWithFilter(
                        viewModel.searchFilter.copy(category = null, modloader = null)
                    )
                },
                searchName = viewModel.searchFilter.searchName,
                onSearchNameChange = {
                    viewModel.researchWithFilter(
                        viewModel.searchFilter.copy(searchName = it)
                    )
                },
                gameVersion = viewModel.searchFilter.gameVersion,
                onGameVersionChange = {
                    viewModel.researchWithFilter(
                        viewModel.searchFilter.copy(gameVersion = it)
                    )
                },
                sortField = viewModel.searchFilter.sortField,
                onSortFieldChange = {
                    viewModel.researchWithFilter(
                        viewModel.searchFilter.copy(sortField = it)
                    )
                },
                categories = categories,
                category = viewModel.searchFilter.category,
                onCategoryChange = {
                    viewModel.researchWithFilter(
                        viewModel.searchFilter.copy(category = it)
                    )
                },
                enableModLoader = enableModLoader,
                modloaders = modloaders,
                modloader = viewModel.searchFilter.modloader,
                onModLoaderChange = {
                    viewModel.researchWithFilter(
                        viewModel.searchFilter.copy(modloader = it)
                    )
                }
            )
        }
    }
}
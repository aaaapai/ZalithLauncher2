package com.movtery.zalithlauncher.ui.screens.content.download.assets.elements

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowLeft
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformFilterCode
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearchData
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.CurseForgeSearchResult
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeData
import com.movtery.zalithlauncher.game.download.assets.platform.mcmod.models.McModSearchItem
import com.movtery.zalithlauncher.game.download.assets.platform.mcmod.models.McModSearchRes
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.ModrinthSearchResult
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.utils.formatNumberByLocale

sealed interface SearchAssetsState {
    data object Searching: SearchAssetsState
    data class Success(val page: AssetsPage): SearchAssetsState
    data class Error(val message: Int, val args: Array<Any>? = null): SearchAssetsState {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Error

            if (message != other.message) return false
            if (!args.contentEquals(other.args)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = message
            result = 31 * result + (args?.contentHashCode() ?: 0)
            return result
        }
    }
}

/**
 * 资源搜索结果展示列表
 * @param mapCategories 通过平台获取类别本地化信息
 * @param mapModLoaders Modrinth将模组加载器信息包含在Categories内，单独获取
 * @param swapToDownload 跳转到下载详情页
 */
@Composable
fun ResultListLayout(
    modifier: Modifier = Modifier,
    searchState: SearchAssetsState,
    onReload: () -> Unit = {},
    onPreviousPage: (pageNumber: Int) -> Unit,
    onNextPage: (pageNumber: Int, isLastPage: Boolean) -> Unit,
    mapCategories: (Platform, String) -> PlatformFilterCode?,
    mapModLoaders: (String) -> PlatformDisplayLabel? = { null },
    swapToDownload: (Platform, projectId: String) -> Unit = { _, _ -> }
) {
    when (val state = searchState) {
        is SearchAssetsState.Searching -> {
            Box(modifier.padding(all = 12.dp)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
        is SearchAssetsState.Success -> {
            val page = state.page
            val result = page.result
            val mcmod = page.mcmod
            val data = when (result) {
                is CurseForgeSearchResult -> result.data
                is ModrinthSearchResult -> result.hits
                else -> error("Unknown result type $result")
            }

            Box(modifier = modifier) {
                ResultList(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 60.dp, bottom = 6.dp),
                    data = data,
                    mcmod = mcmod,
                    mapCategories = mapCategories,
                    mapModLoaders = mapModLoaders,
                    swapToDownload = swapToDownload
                )

                PageController(
                    modifier = Modifier
                        .height(54.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 12.dp, end = 12.dp),
                    page = page,
                    onPreviousPage = {
                        onPreviousPage(page.pageNumber)
                    },
                    onNextPage = {
                        onNextPage(page.pageNumber, page.isLastPage)
                    }
                )
            }
        }
        is SearchAssetsState.Error -> {
            Box(modifier.padding(all = 12.dp)) {
                val message = if (state.args != null) {
                    stringResource(state.message, *state.args)
                } else {
                    stringResource(state.message)
                }

                ScalingLabel(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(R.string.download_assets_failed_to_get_result, message),
                    onClick = onReload
                )
            }
        }
    }
}

@Composable
private fun PageController(
    modifier: Modifier = Modifier,
    page: AssetsPage,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation
    ) {
        Row(
            modifier = Modifier.padding(all = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = "${page.pageNumber} / ${page.totalPage}",
                style = MaterialTheme.typography.labelLarge
            )

            IconButton(
                enabled = page.pageNumber > 1, //不是第一页
                onClick = onPreviousPage
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowLeft,
                    contentDescription = stringResource(R.string.download_assets_result_previous_page)
                )
            }

            IconButton(
                enabled = !page.isLastPage, //不是最后一页
                onClick = onNextPage
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowRight,
                    contentDescription = stringResource(R.string.download_assets_result_next_page)
                )
            }
        }
    }
}

@Composable
private fun ResultList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    data: Array<out PlatformSearchData>,
    mcmod: McModSearchRes?,
    mapCategories: (Platform, String) -> PlatformFilterCode?,
    mapModLoaders: (String) -> PlatformDisplayLabel? = { null },
    swapToDownload: (Platform, projectId: String) -> Unit = { _, _ -> }
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        items(data) { item ->
            val itemModifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)

            when (item) {
                is ModrinthSearchResult.ModrinthProject -> {
                    val modloaders = item.displayCategories
                        ?.mapNotNull {
                            mapModLoaders(it)
                        }
                        ?.toSet()
                        ?.takeIf { it.isNotEmpty() }

                    val categories = item.displayCategories
                        ?.mapNotNull {
                            mapCategories(Platform.MODRINTH, it)
                        }
                        ?.toSet()
                        ?.takeIf { it.isNotEmpty() }
                        ?: item.categories
                            ?.take(4) //没有主要类别，则展示前4个
                            ?.mapNotNull {
                                mapCategories(Platform.MODRINTH, it)
                            }
                            ?.toSet()
                            ?.takeIf { it.isNotEmpty() }

                    var title : String = item.title ?: ""
                    var text : String = item.description ?: ""
                    if (mcmod != null && mcmod.res == 100 && mcmod.data?.get(item.projectId) is McModSearchItem) {
                        val mod = mcmod.data[item.projectId]!!
                        title = mod.mcmod_name
                        text = mod.mcmod_text
                    }

                    ResultItemLayout(
                        modifier = itemModifier,
                        platform = Platform.MODRINTH,
                        title = title,
                        description = text,
                        iconUrl = item.iconUrl,
                        author = item.author,
                        downloads = item.downloads,
                        follows = item.follows,
                        modloaders = modloaders?.sortedWith { o1, o2 -> o1.index() - o2.index() },
                        categories = categories?.sortedWith { o1, o2 -> o1.index() - o2.index() },
                        onClick = {
                            swapToDownload(Platform.MODRINTH, item.projectId)
                        }
                    )
                }
                is CurseForgeData -> {
                    val modloaders: Set<PlatformDisplayLabel>? = item.latestFilesIndexes.mapNotNull {
                        it.modLoader //通过最新文件获取模组加载器信息
                    }.toSet().takeIf { it.isNotEmpty() }

                    val categories = item.categories.mapNotNull {
                        mapCategories(Platform.CURSEFORGE, it.id.toString())
                    }.toSet().takeIf { it.isNotEmpty() }

                    var title : String = item.name
                    var text : String = item.summary
                    if (mcmod != null && mcmod.res == 100 && mcmod.data?.get(item.id.toString()) is McModSearchItem) {
                        val mod = mcmod.data[item.id.toString()]!!
                        title = mod.mcmod_name
                        text = mod.mcmod_text
                    }

                    ResultItemLayout(
                        modifier = itemModifier,
                        platform = Platform.CURSEFORGE,
                        title = title,
                        description = text,
                        iconUrl = item.logo.url,
                        author = item.authors[0].name,
                        downloads = item.downloadCount,
                        modloaders = modloaders?.sortedWith { o1, o2 -> o1.index() - o2.index() },
                        categories = categories?.sortedWith { o1, o2 -> o1.index() - o2.index() },
                        onClick = {
                            swapToDownload(Platform.CURSEFORGE, item.id.toString())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultItemLayout(
    modifier: Modifier = Modifier,
    platform: Platform,
    title: String,
    description: String,
    iconUrl: String? = null,
    author: String? = null,
    downloads: Long = 0L,
    follows: Long? = null,
    modloaders: List<PlatformDisplayLabel>? = null,
    categories: List<PlatformFilterCode>? = null,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(all = 8.dp)
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssetsIcon(
                modifier = Modifier
                    .size(72.dp)
                    .clip(shape = RoundedCornerShape(10.dp))
                    .align(Alignment.CenterVertically),
                iconUrl = iconUrl
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProjectTitleHead(
                    platform = platform,
                    title = title,
                    author = author
                )

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    //描述
                    Text(
                        modifier = Modifier.weight(1f),
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    //下载量、收藏量
                    Column(
                        modifier = Modifier.alpha(0.7f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null
                            )
                            Text(
                                text = formatNumberByLocale(context, downloads),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        follows?.let {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.size(14.dp),
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription = null
                                )
                                Text(
                                    text = formatNumberByLocale(context, it),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                //标签栏
                FlowRow(
                    modifier = Modifier.alpha(0.7f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modloaders?.let {
                        it.forEach { modloader ->
                            Text(
                                text = modloader.getDisplayName(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    categories?.let {
                        it.forEach { category ->
                            Text(
                                text = stringResource(category.getDisplayName()),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectTitleHead(
    modifier: Modifier = Modifier,
    platform: Platform,
    title: String,
    author: String?
) {
    //标题栏、作者栏、平台标签
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //标题栏、作者栏
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .basicMarquee(iterations = Int.MAX_VALUE),
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            author?.let {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Text(
                    modifier = Modifier.alpha(0.7f),
                    text = stringResource(R.string.download_assets_result_authors, it),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
        //平台标签
        PlatformIdentifier(platform = platform)
    }
}
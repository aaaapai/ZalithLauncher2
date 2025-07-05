package com.movtery.zalithlauncher.ui.screens.content.download.assets.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.outlined.ImportContacts
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject
import com.movtery.zalithlauncher.game.download.assets.platform.getProject
import com.movtery.zalithlauncher.game.download.assets.platform.getVersions
import com.movtery.zalithlauncher.game.download.assets.type.RELEASE_REGEX
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.ContentCheckBox
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.ShimmerBox
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.content.DownloadScreenKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.AssetsIcon
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.AssetsVersionItemLayout
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.DownloadAssetsState
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.DownloadProjectInfo
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.DownloadProjectInfo.Urls.Companion.isAllNull
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.DownloadVersionInfo
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.ScreenshotItemLayout
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.VersionInfoMap
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.mapToInfos
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.mapWithVersions
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.toInfo
import com.movtery.zalithlauncher.ui.screens.main.elements.mainScreenKey
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.network.NetWorkUtils
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class DownloadAssetsScreenKey(
    val platform: Platform,
    val projectId: String,
    val classes: PlatformClasses
): NavKey

private class ScreenViewModel(
    private val platform: Platform,
    private val projectId: String,
    private val classes: PlatformClasses
): ViewModel() {
    //版本
    private var _versionsList by mutableStateOf<List<VersionInfoMap>>(emptyList())
    var versionsResult by mutableStateOf<DownloadAssetsState<List<VersionInfoMap>>>(DownloadAssetsState.Getting())

    var showOnlyMCRelease by mutableStateOf(true)
    var searchMCVersion by mutableStateOf("")

    fun filterWith(
        showOnlyMCRelease: Boolean = this.showOnlyMCRelease,
        searchMCVersion: String = this.searchMCVersion
    ) {
        this.showOnlyMCRelease = showOnlyMCRelease
        this.searchMCVersion = searchMCVersion
        viewModelScope.launch {
            val infos = _versionsList.filterInfos()
            versionsResult = DownloadAssetsState.Success(infos)
        }
    }

    private fun List<VersionInfoMap>.filterInfos(): List<VersionInfoMap> {
        return filter { info ->
            (!showOnlyMCRelease || RELEASE_REGEX.matcher(info.gameVersion).find()) &&
                    (searchMCVersion.isEmpty() || info.gameVersion.contains(searchMCVersion, true))
        }
    }

    fun getVersions() {
        viewModelScope.launch {
            versionsResult = DownloadAssetsState.Getting()
            getVersions(
                projectID = projectId,
                platform = platform,
                onSuccess = { result ->
                    val infos: List<DownloadVersionInfo> = result.mapToInfos(projectId) { info ->
                        if (classes != PlatformClasses.MOD) return@mapToInfos //暂时仅支持模组获取依赖
                        info.dependencies.forEach { dependency ->
                            cacheDependencyProject(
                                platform = dependency.platform,
                                projectId = dependency.projectID
                            )
                        }
                    }
                    _versionsList = infos.mapWithVersions()
                    versionsResult = DownloadAssetsState.Success(_versionsList.filterInfos())
                },
                onError = {
                    versionsResult = it
                }
            )
        }
    }

    //项目信息
    var projectResult by mutableStateOf<DownloadAssetsState<DownloadProjectInfo>>(DownloadAssetsState.Getting())

    fun getProject() {
        viewModelScope.launch {
            projectResult = DownloadAssetsState.Getting()
            getProject(
                projectID = projectId,
                platform = platform,
                onSuccess = { result ->
                    projectResult = DownloadAssetsState.Success(result.toInfo(classes))
                },
                onError = { state, _ ->
                    projectResult = state
                }
            )
        }
    }

    //缓存依赖项目
    val cachedDependencyProject = mutableStateMapOf<String, DownloadProjectInfo>()
    //该依赖项目未找到，但是多个版本同时依赖这个不存在的项目
    //就会进行很多次无效的访问，非常耗时
    //需要记录不存在的依赖项目的id，避免下次继续获取
    val notFoundDependencyProjects = mutableStateListOf<String>()

    /**
     * 缓存依赖项目
     */
    private suspend fun cacheDependencyProject(
        platform: Platform,
        projectId: String
    ) {
        if (!notFoundDependencyProjects.contains(projectId) && !cachedDependencyProject.containsKey(projectId)) {
            getProject<PlatformProject>(
                projectID = projectId,
                platform = platform,
                onSuccess = { result ->
                    cachedDependencyProject[projectId] = result.toInfo(classes)
                },
                onError = { _, e ->
                    if (e is ClientRequestException && e.response.status.value == 404) {
                        // 404 Not Found
                        notFoundDependencyProjects.add(projectId)
                    } else {
                        cachedDependencyProject.remove(projectId)
                    }
                }
            )
        }
    }

    init {
        //初始化后，获取项目、版本信息
        getVersions()
        getProject()
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}

@Composable
private fun rememberDownloadAssetsViewModel(
    key: DownloadAssetsScreenKey
): ScreenViewModel {
    return viewModel(
        key = key.toString()
    ) {
        ScreenViewModel(
            platform = key.platform,
            projectId = key.projectId,
            classes = key.classes
        )
    }
}

/**
 * @param parentScreenKey 父屏幕Key
 * @param parentCurrentKey 父屏幕当前Key
 * @param currentKey 当前的Key
 */
@Composable
fun DownloadAssetsScreen(
    parentScreenKey: NavKey,
    parentCurrentKey: NavKey?,
    currentKey: NavKey?,
    key: DownloadAssetsScreenKey,
    onItemClicked: (DownloadVersionInfo) -> Unit = {},
    onDependencyClicked: (DownloadVersionInfo.Dependency) -> Unit = {}
) {
    val viewModel: ScreenViewModel = rememberDownloadAssetsViewModel(key)

    BaseScreen(
        levels1 = listOf(
            Pair(DownloadScreenKey::class.java, mainScreenKey),
            Pair(DownloadAssetsScreenKey::class.java, currentKey)
        ),
        Triple(parentScreenKey, parentCurrentKey, false)
    ) { isVisible ->
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            val yOffset by swapAnimateDpAsState(targetValue = (-40).dp, swapIn = isVisible)
            Versions(
                modifier = Modifier
                    .weight(6.5f)
                    .fillMaxHeight()
                    .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                viewModel = viewModel,
                onReload = { viewModel.getVersions() },
                onItemClicked = onItemClicked,
                onDependencyClicked = onDependencyClicked
            )

            val xOffset by swapAnimateDpAsState(
                targetValue = 40.dp,
                swapIn = isVisible,
                isHorizontal = true
            )
            ProjectInfo(
                modifier = Modifier
                    .weight(3.5f)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .padding(end = 12.dp)
                    .offset { IntOffset(x = xOffset.roundToPx(), y = 0) },
                projectResult = viewModel.projectResult,
                onReload = { viewModel.getProject() }
            )
        }
    }
}

/**
 * 所有版本列表
 */
@Composable
private fun Versions(
    modifier: Modifier = Modifier,
    viewModel: ScreenViewModel,
    onReload: () -> Unit = {},
    onItemClicked: (DownloadVersionInfo) -> Unit = {},
    onDependencyClicked: (DownloadVersionInfo.Dependency) -> Unit = {}
) {
    when (val versions = viewModel.versionsResult) {
        is DownloadAssetsState.Getting -> {
            Box(modifier.padding(all = 12.dp)) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
        is DownloadAssetsState.Success -> {
            Column(
                modifier = modifier
            ) {
                //简单过滤条件
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContentCheckBox(
                        checked = viewModel.showOnlyMCRelease,
                        onCheckedChange = { viewModel.filterWith(showOnlyMCRelease = it) }
                    ) {
                        Text(
                            text = stringResource(R.string.download_assets_show_only_mc_release),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    SimpleTextInputField(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        value = viewModel.searchMCVersion,
                        onValueChange = { viewModel.filterWith(searchMCVersion = it) },
                        color = itemLayoutColor(),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        singleLine = true,
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface).copy(fontSize = 12.sp),
                        hint = {
                            Text(
                                text = stringResource(R.string.download_assets_search_mc_versions),
                                style = TextStyle(color = MaterialTheme.colorScheme.onSurface).copy(fontSize = 12.sp)
                            )
                        }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val scrollState = rememberLazyListState()

                LaunchedEffect(Unit) {
                    versions.result.indexOfFirst { it.isAdapt }.takeIf { it != -1 }?.let { index ->
                        //自动滚动到适配的资源版本
                        scrollState.animateScrollToItem(index)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    state = scrollState
                ) {
                    items(versions.result) { info ->
                        AssetsVersionItemLayout(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            infoMap = info,
                            getDependency = { projectId ->
                                viewModel.cachedDependencyProject[projectId]
                            },
                            onItemClicked = onItemClicked,
                            onDependencyClicked = onDependencyClicked
                        )
                    }
                }
            }
        }
        is DownloadAssetsState.Error -> {
            Box(modifier.padding(all = 12.dp)) {
                val message = if (versions.args != null) {
                    stringResource(versions.message, *versions.args)
                } else {
                    stringResource(versions.message)
                }

                ScalingLabel(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(R.string.download_assets_failed_to_get_versions, message),
                    onClick = onReload
                )
            }
        }
    }
}

/**
 * 项目信息板块
 */
@Composable
private fun ProjectInfo(
    modifier: Modifier = Modifier,
    projectResult: DownloadAssetsState<DownloadProjectInfo>,
    onReload: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        when (val result = projectResult) {
            is DownloadAssetsState.Getting -> {
                LazyColumn(
                    contentPadding = PaddingValues(all = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    //图标、标题、简介的骨架
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ShimmerBox(
                                modifier = Modifier
                                    .clip(shape = RoundedCornerShape(10.dp))
                                    .size(72.dp)
                            )
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                //标题
                                ShimmerBox(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                //简介
                                ShimmerBox(
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
            is DownloadAssetsState.Success -> {
                val info = result.result

                LazyColumn(
                    contentPadding = PaddingValues(all = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    //图标、标题、简介
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AssetsIcon(
                                modifier = Modifier
                                    .clip(shape = RoundedCornerShape(10.dp))
                                    .size(72.dp),
                                iconUrl = info.iconUrl
                            )
                            //标题、简介
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = info.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                                info.summary?.let { summary ->
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    //相关链接
                    if (!info.urls.isAllNull()) {
                        item {
                            val context = LocalContext.current
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.download_assets_links),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                info.urls.projectUrl?.let { url ->
                                    IconTextButton(
                                        onClick = { NetWorkUtils.openLink(context, url) },
                                        iconSize = 18.dp,
                                        painter = when (info.platform) {
                                            Platform.CURSEFORGE -> painterResource(R.drawable.ic_curseforge)
                                            Platform.MODRINTH -> painterResource(R.drawable.ic_modrinth)
                                        },
                                        text = stringResource(R.string.download_assets_project_link)
                                    )
                                }
                                info.urls.sourceUrl?.let { url ->
                                    IconTextButton(
                                        onClick = { NetWorkUtils.openLink(context, url) },
                                        iconSize = 18.dp,
                                        imageVector = Icons.Default.Code,
                                        text = stringResource(R.string.download_assets_source_link)
                                    )
                                }
                                info.urls.issuesUrl?.let { url ->
                                    IconTextButton(
                                        onClick = { NetWorkUtils.openLink(context, url) },
                                        iconSize = 18.dp,
                                        painter = painterResource(R.drawable.ic_chat_info),
                                        text = stringResource(R.string.download_assets_issues_link)
                                    )
                                }
                                info.urls.wikiUrl?.let { url ->
                                    IconTextButton(
                                        onClick = { NetWorkUtils.openLink(context, url) },
                                        iconSize = 18.dp,
                                        imageVector = Icons.Outlined.ImportContacts,
                                        text = stringResource(R.string.download_assets_wiki_link)
                                    )
                                }
                            }
                        }
                    }

                    //屏幕截图
                    items(info.screenshots) { screenshot ->
                        ScreenshotItemLayout(
                            modifier = Modifier.fillMaxWidth(),
                            screenshot = screenshot
                        )
                    }
                }
            }
            is DownloadAssetsState.Error -> {
                Box(Modifier
                    .fillMaxSize()
                    .padding(all = 12.dp)) {
                    val message = if (result.args != null) {
                        stringResource(result.message, *result.args)
                    } else {
                        stringResource(result.message)
                    }

                    ScalingLabel(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(R.string.download_assets_failed_to_get_project, message),
                        onClick = onReload
                    )
                }
            }
        }
    }
}
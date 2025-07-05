package com.movtery.zalithlauncher.ui.screens.content.download.assets.elements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformClasses
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDependencyType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformDisplayLabel
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformProject
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformReleaseType
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformSearch
import com.movtery.zalithlauncher.game.download.assets.platform.PlatformVersion
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeFile
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeFile.Companion.fixedFileUrl
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.curseforge.models.CurseForgeProject
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthModLoaderCategory
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthSingleProject
import com.movtery.zalithlauncher.game.download.assets.platform.modrinth.models.ModrinthVersion
import com.movtery.zalithlauncher.game.download.assets.type.RELEASE_REGEX
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.components.LittleTextLabel
import com.movtery.zalithlauncher.ui.components.ShimmerBox
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.components.rememberMaxHeight
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.formatNumberByLocale
import com.movtery.zalithlauncher.utils.getTimeAgo
import com.movtery.zalithlauncher.utils.logging.Logger.lWarning
import com.movtery.zalithlauncher.utils.string.compareVersion
import kotlinx.serialization.SerializationException
import java.io.FileNotFoundException
import java.io.IOException

sealed interface DownloadAssetsState<T> {
    class Getting<T> : DownloadAssetsState<T>
    data class Success<T>(val result: T) : DownloadAssetsState<T>
    data class Error<T>(val message: Int, val args: Array<Any>? = null) : DownloadAssetsState<T> {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Error<*>

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
 * 下载资源项目通用详细信息
 */
class DownloadProjectInfo(
    val platform: Platform,
    val iconUrl: String? = null,
    val title: String,
    val summary: String? = null,
    val author: String? = null,
    val downloadCount: Long,
    val urls: Urls,
    val screenshots: List<Screenshot> = emptyList()
) {
    /**
     * 资源项目各类外链
     * @param projectUrl 平台项目链接
     * @param sourceUrl 源代码仓库链接
     * @param issuesUrl 议题链接
     * @param wikiUrl wiki链接
     */
    class Urls(
        val projectUrl: String? = null,
        val sourceUrl: String? = null,
        val issuesUrl: String? = null,
        val wikiUrl: String? = null
    ) {
        companion object {
            fun Urls.isAllNull(): Boolean {
                return projectUrl == null &&
                        sourceUrl == null &&
                        issuesUrl == null &&
                        wikiUrl   == null
            }
        }
    }

    /**
     * 屏幕截图
     * @param imageUrl 图片链接
     * @param title 截图标题
     * @param description 截图描述
     */
    class Screenshot(
        val imageUrl: String,
        val title: String? = null,
        val description: String? = null
    )
}

/**
 * 下载资源版本通用详细信息
 */
class DownloadVersionInfo(
    val platform: Platform,
    val displayName: String,
    val fileName: String,
    val gameVersion: Array<String>,
    val loaders: List<PlatformDisplayLabel>,
    val releaseType: PlatformReleaseType,
    val dependencies: List<Dependency>,
    val downloadCount: Long,
    val downloadUrl: String,
    val date: String,
    val sha1: String? = null,
    val fileSize: Long
) {
    class Dependency(
        val platform: Platform,
        val projectID: String,
        val type: PlatformDependencyType
    )
}

/**
 * 版本、模组加载器 版本信息分组
 */
class VersionInfoMap(
    val gameVersion: String,
    val loader: PlatformDisplayLabel?,
    val dependencies: List<DownloadVersionInfo.Dependency>,
    val infos: List<DownloadVersionInfo>,
    val isAdapt: Boolean
)

suspend fun List<PlatformVersion>.mapToInfos(
    currentProjectId: String,
    also: suspend (DownloadVersionInfo) -> Unit = {}
): List<DownloadVersionInfo> {
    return mapNotNull { version ->
        when (version) {
            is ModrinthVersion -> {
                val files = version.files.takeIf { it.isNotEmpty() } ?: run {
                    lWarning("No file list available, skipping -> ${version.name}")
                    return@mapNotNull null
                }
                val file = files.find { it.primary } ?: files[0] //仅下载主文件
                DownloadVersionInfo(
                    platform = Platform.MODRINTH,
                    displayName = version.name,
                    fileName = file.fileName,
                    gameVersion = version.gameVersions,
                    loaders = version.loaders.mapNotNull { loaderName ->
                        ModrinthModLoaderCategory.entries.find { it.facetValue() == loaderName }
                    },
                    releaseType = version.versionType,
                    dependencies = version.dependencies.mapNotNull { dependency ->
                        DownloadVersionInfo.Dependency(
                            platform = Platform.MODRINTH,
                            projectID = dependency.projectId ?: return@mapNotNull null,
                            type = dependency.dependencyType
                        )
                    },
                    downloadCount = version.downloads,
                    downloadUrl = file.url,
                    date = version.datePublished,
                    sha1 = file.hashes.sha1,
                    fileSize = file.size
                )
            }
            is CurseForgeFile -> {
                val file = version.takeIf { it.fileName != null && it.fixedFileUrl() != null }
                // 文件名或者下载链接为空
                // 单独获取该文件信息
                    ?: run {
                        val fileId = version.id.toString()
                        runCatching {
                            PlatformSearch.getVersionFromCurseForge(
                                projectID = currentProjectId,
                                fileID = fileId
                            ).data
                        }.onFailure { e ->
                            when (e) {
                                is FileNotFoundException -> lWarning("Could not query api.curseforge.com for deleted mods: $currentProjectId, $fileId", e)
                                is IOException, is SerializationException -> lWarning("Unable to fetch the file name projectID=$currentProjectId, fileID=$fileId", e)
                            }
                        }.getOrNull() ?: return@mapNotNull null
                    }

                val downloadUrl = file.fixedFileUrl() ?: run {
                    lWarning("No download link available, projectID=$currentProjectId, fileID=${file.id}")
                    return@mapNotNull null
                }

                val gameVersions = file.gameVersions.filter { gameVersion ->
                    RELEASE_REGEX.matcher(gameVersion).find()
                }.toTypedArray()

                val sha1 = file.hashes.find { hash ->
                    hash.algo == CurseForgeFile.Hash.Algo.SHA1
                }

                DownloadVersionInfo(
                    platform = Platform.CURSEFORGE,
                    displayName = file.displayName,
                    fileName = file.fileName!!,
                    gameVersion = gameVersions,
                    loaders = file.gameVersions.mapNotNull { loaderName ->
                        CurseForgeModLoader.entries.find {
                            it.getDisplayName().equals(loaderName, true)
                        }
                    },
                    releaseType = file.releaseType,
                    dependencies = file.dependencies.map { dependency ->
                        DownloadVersionInfo.Dependency(
                            platform = Platform.CURSEFORGE,
                            projectID = dependency.modId.toString(),
                            type = dependency.relationType
                        )
                    },
                    downloadCount = file.downloadCount,
                    downloadUrl = downloadUrl,
                    date = file.fileDate,
                    sha1 = sha1?.value,
                    fileSize = file.fileLength
                )
            }
            else -> error("Unknown version type: $version")
        }.also {
            also(it)
        }
    }
}

fun PlatformProject.toInfo(
    platformClasses: PlatformClasses
): DownloadProjectInfo {
    return when (this) {
        is ModrinthSingleProject -> {
            DownloadProjectInfo(
                platform = Platform.MODRINTH,
                iconUrl = iconUrl,
                title = title,
                summary = description,
                downloadCount = downloads,
                urls = DownloadProjectInfo.Urls(
                    projectUrl = "https://modrinth.com/${platformClasses.modrinth!!.facetValue()}/${slug}",
                    sourceUrl = sourceUrl,
                    issuesUrl = issuesUrl,
                    wikiUrl = wikiUrl
                ),
                screenshots = gallery.map { gallery ->
                    DownloadProjectInfo.Screenshot(
                        imageUrl = gallery.url,
                        title = gallery.title,
                        description = gallery.description
                    )
                }
            )
        }
        is CurseForgeProject -> {
            val data = data
            DownloadProjectInfo(
                platform = Platform.CURSEFORGE,
                iconUrl = data.logo.url,
                title = data.name,
                summary = data.summary,
                author = data.authors[0].name,
                downloadCount = data.downloadCount,
                urls = DownloadProjectInfo.Urls(
                    projectUrl = "https://www.curseforge.com/minecraft/${platformClasses.curseforge.slug}/${data.slug}",
                    sourceUrl = data.links.sourceUrl,
                    issuesUrl = data.links.issuesUrl,
                    wikiUrl = data.links.wikiUrl
                ),
                screenshots = data.screenshots.map { screenshot ->
                    DownloadProjectInfo.Screenshot(
                        imageUrl = screenshot.url,
                        title = screenshot.title,
                        description = screenshot.description
                    )
                }
            )
        }
        else -> error("Unknown project type: $this")
    }
}

fun List<DownloadVersionInfo>.mapWithVersions(): List<VersionInfoMap> {
    val grouped = mutableMapOf<Pair<String, PlatformDisplayLabel?>, MutableList<DownloadVersionInfo>>()

    forEach { versionInfo ->
        val labels = versionInfo.loaders.ifEmpty { listOf(null) }
        versionInfo.gameVersion.forEach { gameVer ->
            labels.forEach { loaderLabel ->
                grouped.getOrPut(gameVer to loaderLabel) { mutableListOf() } += versionInfo
            }
        }
    }

    return grouped.map { (key, infos) ->
        //去重依赖集合
        val dependencies = infos
            .flatMap { it.dependencies }
            .distinctBy { dep -> Triple(dep.platform, dep.projectID, dep.type) }

        VersionInfoMap(
            gameVersion = key.first,
            loader = key.second,
            dependencies = dependencies,
            infos = infos,
            isAdapt = isVersionAdapt(key.first, key.second)
        )
    }.sortedByVersionAndLoader()
}

private fun List<VersionInfoMap>.sortedByVersionAndLoader(): List<VersionInfoMap> {
    return sortedWith { a, b ->
        // 比较版本号
        val versionCompare = -a.gameVersion.compareVersion(b.gameVersion)
        if (versionCompare != 0) {
            versionCompare
        } else {
            when {
                a.loader == null && b.loader == null -> 0
                a.loader == null -> 1
                b.loader == null -> -1
                else -> a.loader.getDisplayName().compareTo(b.loader.getDisplayName())
            }
        }
    }
}

/**
 * 当前资源版本是否与当前选择的游戏版本匹配
 */
private fun isVersionAdapt(gameVersion: String, loader: PlatformDisplayLabel?): Boolean {
    val currentVersion = VersionsManager.currentVersion
    return if (currentVersion == null) {
        false //没安装版本，无法判断
    } else {
        if (currentVersion.getVersionInfo()?.minecraftVersion != gameVersion) {
            false //游戏版本不匹配
        } else {
            //判断模组加载器匹配情况
            val loaderInfo = currentVersion.getVersionInfo()?.loaderInfo
            when {
                loader == null -> true //资源没有模组加载器信息，直接判定适配
                loaderInfo == null -> false //资源有模组加载器，但当前版本没有模组加载器信息，不适配
                else -> loaderInfo.name.equals(loader.getDisplayName(), true)
            }
        }
    }
}

/**
 * 资源版本分组可折叠列表
 */
@Composable
fun AssetsVersionItemLayout(
    modifier: Modifier = Modifier,
    infoMap: VersionInfoMap,
    getDependency: (projectId: String) -> DownloadProjectInfo?,
    maxListHeight: Dp = rememberMaxHeight(),
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp,
    onItemClicked: (DownloadVersionInfo) -> Unit = {},
    onDependencyClicked: (DownloadVersionInfo.Dependency) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AssetsVersionHeadLayout(
                modifier = Modifier.fillMaxWidth(),
                infoMap = infoMap,
                isAdapt = infoMap.isAdapt,
                expanded = expanded,
                onClick = { expanded = !expanded }
            )

            if (infoMap.infos.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(animationSpec = getAnimateTween()),
                        exit = shrinkVertically(animationSpec = getAnimateTween()) + fadeOut(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxListHeight)
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            infoMap.dependencies.takeIf { it.isNotEmpty() }?.let { dependencies ->
                                val projects = dependencies.mapNotNull { dependency ->
                                    if (dependency.type == PlatformDependencyType.REQUIRED) {
                                        getDependency(dependency.projectID)?.let { dependency to it }
                                    } else null
                                }
                                if (projects.isNotEmpty()) {
                                    item {
                                        Text(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            text = stringResource(R.string.download_assets_dependency_projects),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                    //前置项目列表
                                    items(projects) { (dependency, dependencyProject) ->
                                        AssetsVersionDependencyItem(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(all = 4.dp),
                                            project = dependencyProject,
                                            onClick = {
                                                onDependencyClicked(dependency)
                                            }
                                        )
                                    }
                                    item {
                                        HorizontalDivider(
                                            modifier = Modifier
                                                .padding(horizontal = 12.dp)
                                                .fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }

                            items(infoMap.infos) { info ->
                                AssetsVersionListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(all = 4.dp),
                                    info = info,
                                    onClick = {
                                        onItemClicked(info)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetsVersionHeadLayout(
    modifier: Modifier = Modifier,
    infoMap: VersionInfoMap,
    isAdapt: Boolean,
    expanded: Boolean,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(vertical = 8.dp),
                text = infoMap.gameVersion,
                style = MaterialTheme.typography.titleSmall
            )
            infoMap.loader?.let { loader ->
                LittleTextLabel(
                    text = loader.getDisplayName(),
                    shape = MaterialTheme.shapes.small
                )
            }
        }
        if (isAdapt) {
            Icon(
                modifier = Modifier.size(18.dp),
                imageVector = Icons.Filled.Star,
                contentDescription = null
            )
        }
        if (!infoMap.infos.isEmpty()) {
            Row(
                modifier = Modifier.padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) -180f else 0f,
                    animationSpec = getAnimateTween()
                )
                Icon(
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotation),
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = stringResource(if (expanded) R.string.generic_expand else R.string.generic_collapse)
                )
            }
        }
    }
}

@Composable
private fun AssetsVersionDependencyItem(
    modifier: Modifier = Modifier,
    project: DownloadProjectInfo,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetsIcon(
            modifier = Modifier
                .padding(all = 8.dp)
                .clip(shape = RoundedCornerShape(10.dp))
                .size(42.dp),
            iconUrl = project.iconUrl
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            ProjectTitleHead(
                platform = project.platform,
                title = project.title,
                author = project.author
            )
            project.summary?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(2.dp))
    }
}

@Composable
private fun AssetsVersionListItem(
    modifier: Modifier = Modifier,
    info: DownloadVersionInfo,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //直观的版本状态
        Box(
            modifier = Modifier
                .padding(start = 12.dp, end = 8.dp)
                .size(34.dp)
                .clip(shape = CircleShape)
                .background(info.releaseType.color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = info.releaseType.name.take(1),
                style = MaterialTheme.typography.labelLarge,
                color = info.releaseType.color
            )
        }

        //版本简要信息
        Column(
            modifier = Modifier.padding(all = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = info.displayName,
                style = MaterialTheme.typography.labelLarge
            )

            val context = LocalContext.current

            Row(
                modifier = Modifier.alpha(0.7f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                //下载量
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
                        text = formatNumberByLocale(context, info.downloadCount),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                //更新时间
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = Icons.Outlined.Autorenew,
                        contentDescription = null
                    )
                    Text(
                        text = getTimeAgo(
                            context = LocalContext.current,
                            dateString = info.date
                        ),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                //版本状态
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.ic_package_2),
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(info.releaseType.textRes),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/**
 * 屏幕截图与描述UI
 */
@Composable
fun ScreenshotItemLayout(
    modifier: Modifier = Modifier,
    screenshot: DownloadProjectInfo.Screenshot
) {
    val context = LocalContext.current

    val imageRequest = remember(screenshot) {
        screenshot.imageUrl.takeIf { it.isNotBlank() }?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .build()
        }
    }

    val painter = rememberAsyncImagePainter(
        model = imageRequest,
        placeholder = null,
        error = painterResource(R.drawable.ic_unknown_icon)
    )

    val state by painter.state.collectAsState()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state) {
            AsyncImagePainter.State.Empty -> {
                //NONE
            }
            is AsyncImagePainter.State.Error, is AsyncImagePainter.State.Loading -> {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
            is AsyncImagePainter.State.Success -> {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(painter.intrinsicSize.width / painter.intrinsicSize.height)
                )
            }
        }

        //标题与简介部分
        if (screenshot.title != null && screenshot.title == screenshot.description) {
            //标题与简介内容相同，则不需要两个都显示
            //会有作者喜欢把标题与简介设置成一样的内容
            Text(
                text = screenshot.title,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )
        } else {
            screenshot.title?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
            screenshot.description?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
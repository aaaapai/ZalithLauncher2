package com.movtery.zalithlauncher.ui.screens.content.versions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.assets.platform.Platform
import com.movtery.zalithlauncher.game.download.assets.utils.getMcmodTitle
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.mod.AllModReader
import com.movtery.zalithlauncher.game.version.mod.LocalMod
import com.movtery.zalithlauncher.game.version.mod.LocalMod.Companion.isDisabled
import com.movtery.zalithlauncher.game.version.mod.LocalMod.Companion.isEnabled
import com.movtery.zalithlauncher.game.version.mod.RemoteMod
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.LittleTextLabel
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.components.TooltipIconButton
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.download.assets.elements.AssetsIcon
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ByteArrayIcon
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.LoadingState
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ModsOperation
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.filterMods
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.VersionSettingsBackground
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.file.formatFileSize
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.isNotEmptyOrBlank
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.LinkedList

private class ModsManageViewModel(
    modsDir: File
) : ViewModel() {
    val modReader = AllModReader(modsDir)

    var nameFilter by mutableStateOf("")

    var allMods by mutableStateOf<List<RemoteMod>>(emptyList())
        private set
    var filteredMods by mutableStateOf<List<RemoteMod>?>(null)
        private set

    /** 作为标记，记录哪些模组已被加载 */
    private val modsToLoad = mutableListOf<RemoteMod>()
    private val loadQueue = LinkedList<Pair<RemoteMod, Boolean>>()
    private val semaphore = Semaphore(8) //一次最多允许同时加载8个模组
    private var initialQueueSize = 0
    private val queueMutex = Mutex()

    var modsState by mutableStateOf<LoadingState>(LoadingState.None)

    private var job: Job? = null

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            modsState = LoadingState.Loading
            try {
                allMods = modReader.readAllMods()
                filterMods()
            } catch (_: CancellationException) {
                //已取消
            }
            modsState = LoadingState.None
        }
    }

    init {
        refresh()
        startQueueProcessor()
    }

    fun updateFilter(name: String) {
        this.nameFilter = name
        filterMods()
    }

    private fun filterMods() {
        filteredMods = allMods.takeIf { it.isNotEmpty() }?.filterMods(nameFilter)
    }

    /** 在ViewModel的生命周期协程内调用 */
    fun doInScope(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
        }
    }

    private fun startQueueProcessor() {
        viewModelScope.launch {
            while (true) {
                try {
                    ensureActive()
                } catch (_: Exception) {
                    break //取消
                }

                val task = queueMutex.withLock {
                    loadQueue.poll()
                } ?: run {
                    delay(100)
                    continue
                }

                val (mod, loadFromCache) = task
                semaphore.acquire()

                launch {
                    try {
                        mod.load(loadFromCache)
                    } finally {
                        semaphore.release()
                    }
                }
            }
        }
    }

    /** 加载模组远端信息 */
    fun loadMod(mod: RemoteMod, loadFromCache: Boolean = true) {
        //强制刷新：直接加入队列头部并清除旧任务
        if (!loadFromCache) {
            doInScope {
                queueMutex.withLock {
                    loadQueue.removeAll { it.first == mod }
                    loadQueue.addFirst(mod to false) //加入队头优先执行
                }
            }
            if (modsToLoad.contains(mod)) return //已在加载列表
            modsToLoad.add(mod)
            return
        }

        if (modsToLoad.contains(mod)) return

        modsToLoad.add(mod)
        doInScope {
            queueMutex.withLock {
                val canJoin = loadQueue.size <= (initialQueueSize / 2)
                if (canJoin || loadQueue.none { it.first == mod }) {
                    loadQueue.add(mod to true)
                    modsToLoad.add(mod)
                    //若当前是新一轮任务，更新初始队列总数
                    if (initialQueueSize == 0 || canJoin) {
                        initialQueueSize = loadQueue.size
                    }
                }
            }
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }
}

@Composable
private fun rememberModsManageViewModel(
    version: Version
): ModsManageViewModel {
    val folderName = VersionFolders.MOD.folderName
    return viewModel(
        key = version.toString() + "_" + folderName
    ) {
        ModsManageViewModel(File(version.getGameDir(), folderName))
    }
}

@Composable
fun ModsManagerScreen(
    mainScreenKey: NavKey?,
    versionsScreenKey: NavKey?,
    version: Version,
    swapToDownload: () -> Unit = {},
    onSwapMoreInfo: (id: String, Platform) -> Unit
) {
    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.Versions::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.ModsManager, versionsScreenKey, false)
    ) { isVisible ->
        val viewModel = rememberModsManageViewModel(version)

        val yOffset by swapAnimateDpAsState(
            targetValue = (-40).dp,
            swapIn = isVisible
        )

        VersionSettingsBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 12.dp)
                .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
            paddingValues = PaddingValues()
        ) {
            when (viewModel.modsState) {
                LoadingState.None -> {
                    val itemColor = itemLayoutColor()
                    val itemContentColor = MaterialTheme.colorScheme.onSurface

                    var modsOperation by remember { mutableStateOf<ModsOperation>(ModsOperation.None) }
                    fun runProgress(task: () -> Unit) {
                        viewModel.doInScope {
                            withContext(Dispatchers.IO) {
                                modsOperation = ModsOperation.Progress
                                task()
                                modsOperation = ModsOperation.None
                                viewModel.refresh()
                            }
                        }
                    }
                    ModsOperation(
                        modsOperation = modsOperation,
                        updateOperation = { modsOperation = it },
                        onDelete = { mod ->
                            runProgress {
                                FileUtils.deleteQuietly(mod.file)
                            }
                        }
                    )

                    Column {
                        ModsActionsHeader(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .padding(top = 4.dp)
                                .fillMaxWidth(),
                            inputFieldColor = itemColor,
                            inputFieldContentColor = itemContentColor,
                            nameFilter = viewModel.nameFilter,
                            onNameFilterChange = { viewModel.updateFilter(it) },
                            swapToDownload = swapToDownload,
                            refresh = { viewModel.refresh() }
                        )

                        ModsList(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            modsList = viewModel.filteredMods,
                            onLoad = { mod ->
                                viewModel.loadMod(mod)
                            },
                            onForceRefresh = { mod ->
                                viewModel.loadMod(mod, loadFromCache = false)
                            },
                            onEnable = { mod ->
                                viewModel.doInScope {
                                    withContext(Dispatchers.IO) {
                                        mod.localMod.enable()
                                    }
                                }
                            },
                            onDisable = { mod ->
                                viewModel.doInScope {
                                    withContext(Dispatchers.IO) {
                                        mod.localMod.disable()
                                    }
                                }
                            },
                            onSwapMoreInfo = onSwapMoreInfo,
                            onDelete = { mod ->
                                modsOperation = ModsOperation.Delete(mod.localMod)
                            },
                            itemColor = itemColor,
                            itemContentColor = itemContentColor
                        )
                    }
                }
                LoadingState.Loading -> {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModsActionsHeader(
    modifier: Modifier,
    inputFieldColor: Color,
    inputFieldContentColor: Color,
    nameFilter: String,
    onNameFilterChange: (String) -> Unit = {},
    swapToDownload: () -> Unit = {},
    refresh: () -> Unit = {}
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleTextInputField(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                value = nameFilter,
                onValueChange = { onNameFilterChange(it) },
                hint = {
                    Text(
                        text = stringResource(R.string.generic_search),
                        style = TextStyle(color = LocalContentColor.current).copy(fontSize = 12.sp)
                    )
                },
                color = inputFieldColor,
                contentColor = inputFieldContentColor,
                singleLine = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            IconTextButton(
                onClick = swapToDownload,
                imageVector = Icons.Default.Download,
                text = stringResource(R.string.generic_download)
            )

            IconButton(
                onClick = refresh
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.generic_refresh)
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ModsList(
    modifier: Modifier = Modifier,
    modsList: List<RemoteMod>?,
    onLoad: (RemoteMod) -> Unit,
    onForceRefresh: (RemoteMod) -> Unit,
    onEnable: (RemoteMod) -> Unit,
    onDisable: (RemoteMod) -> Unit,
    onSwapMoreInfo: (id: String, Platform) -> Unit,
    onDelete: (RemoteMod) -> Unit,
    itemColor: Color,
    itemContentColor: Color,
) {
    modsList?.let { list ->
        //如果列表是空的，则是由搜索导致的
        if (list.isNotEmpty()) {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                items(list) { mod ->
                    ModItemLayout(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        mod = mod,
                        onLoad = {
                            onLoad(mod)
                        },
                        onForceRefresh = {
                            onForceRefresh(mod)
                        },
                        onEnable = {
                            onEnable(mod)
                        },
                        onDisable = {
                            onDisable(mod)
                        },
                        onSwapMoreInfo = onSwapMoreInfo,
                        onDelete = {
                            onDelete(mod)
                        },
                        itemColor = itemColor,
                        itemContentColor = itemContentColor
                    )
                }
            }
        }
    } ?: run {
        //如果为null，则代表本身就没有模组可以展示
        Box(modifier = Modifier.fillMaxSize()) {
            ScalingLabel(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(R.string.mods_manage_no_mods)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModItemLayout(
    modifier: Modifier = Modifier,
    mod: RemoteMod,
    onLoad: () -> Unit = {},
    onForceRefresh: () -> Unit = {},
    onClick: () -> Unit = {},
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onSwapMoreInfo: (id: String, Platform) -> Unit,
    onDelete: () -> Unit,
    itemColor: Color,
    itemContentColor: Color,
    shadowElevation: Dp = 1.dp
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }

    val projectInfo = mod.projectInfo

    LaunchedEffect(mod) {
        //尝试加载该模组文件在平台上所属的项目
        onLoad()
    }

    Surface(
        modifier = modifier.graphicsLayer(scaleY = scale.value, scaleX = scale.value),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = itemColor,
        contentColor = itemContentColor,
        shadowElevation = shadowElevation
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            //模组的封面图标
            ModIcon(
                modifier = Modifier.clip(shape = RoundedCornerShape(10.dp)),
                mod = mod,
                iconSize = 48.dp
            )

            //模组简要信息
            Crossfade(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f),
                //在本地是否为未知文件
                targetState = mod.localMod.notMod && projectInfo == null,
                label = "ModItemInfoCrossfade"
            ) { isUnknown ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val localMod = mod.localMod
                    when {
                        isUnknown -> {
                            //非模组，只展示文件名称
                            Text(
                                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                                text = localMod.file.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1
                            )
                            if (localMod.loader != ModLoader.UNKNOWN) {
                                LittleTextLabel(
                                    text = localMod.loader.displayName,
                                    shape = MaterialTheme.shapes.small
                                )
                            }
                        }
                        else -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val displayTitle = if (projectInfo != null) {
                                    val title = projectInfo.title
                                    mod.mcMod?.getMcmodTitle(title) ?: title
                                } else {
                                    localMod.name
                                }
                                Text(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .basicMarquee(iterations = Int.MAX_VALUE)
                                        .animateContentSize(),
                                    text = displayTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1
                                )
                                Row(
                                    modifier = Modifier
                                        .basicMarquee(iterations = Int.MAX_VALUE)
                                        .animateContentSize(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val remoteLoaders = mod.remoteLoaders
                                    if (remoteLoaders != null && remoteLoaders.loaders.isNotEmpty()) {
                                        remoteLoaders.loaders.forEach { loader ->
                                            LittleTextLabel(
                                                text = loader.getDisplayName(),
                                                shape = MaterialTheme.shapes.small
                                            )
                                        }
                                    } else if (localMod.loader != ModLoader.UNKNOWN) {
                                        LittleTextLabel(
                                            text = localMod.loader.displayName,
                                            shape = MaterialTheme.shapes.small
                                        )
                                    }
                                }
                            }

                            Text(
                                modifier = Modifier
                                    .alpha(0.7f)
                                    .basicMarquee(iterations = Int.MAX_VALUE),
                                text = stringResource(R.string.generic_file_name, localMod.file.name),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mod.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .alpha(0.7f),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                } else if (mod.isLoaded) {
                    IconButton(
                        modifier = Modifier.size(38.dp),
                        onClick = onForceRefresh
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.generic_refresh)
                        )
                    }
                }

                //启用/禁用
                Checkbox(
                    checked = mod.localMod.file.isEnabled(),
                    onCheckedChange = { checked ->
                        if (checked) onEnable()
                        else onDisable()
                    }
                )

                //详细信息展示
                if (projectInfo == null) {
                    if (!mod.localMod.notMod) {
                        LocalModInfoTooltip(mod.localMod)
                    }
                } else {
                    IconButton(
                        modifier = Modifier.size(38.dp),
                        onClick = {
                            onSwapMoreInfo(projectInfo.id, projectInfo.platform)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.mods_manage_info)
                        )
                    }
                }

                IconButton(
                    modifier = Modifier.size(38.dp),
                    onClick = onDelete
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.generic_delete)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModIcon(
    modifier: Modifier = Modifier,
    mod: RemoteMod,
    iconSize: Dp,
    disableContainerSize: Dp = 28.dp
) {
    Box(modifier = modifier) {
        val colorMatrix = remember(mod, mod.localMod.file) { ColorMatrix() }
        colorMatrix.setToSaturation(
            if (mod.localMod.file.isDisabled()) 0f
            else 1f
        )

        val projectInfo = mod.projectInfo
        if (projectInfo == null) {
            ByteArrayIcon(
                modifier = Modifier.size(iconSize),
                triggerRefresh = mod,
                icon = mod.localMod.icon,
                colorFilter = ColorFilter.colorMatrix(colorMatrix)
            )
        } else {
            AssetsIcon(
                modifier = Modifier.size(iconSize),
                iconUrl = projectInfo.iconUrl,
                colorFilter = ColorFilter.colorMatrix(colorMatrix)
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = mod.localMod.file.isDisabled(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 4.dp)
                    .size(disableContainerSize),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = CircleShape,
                shadowElevation = 4.dp
            ) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LocalModInfoTooltip(
    mod: LocalMod
) {
    TooltipIconButton(
        modifier = Modifier.size(38.dp),
        tooltip = {
            RichTooltip(
                modifier = Modifier.padding(all = 3.dp),
                title = { Text(text = stringResource(R.string.mods_manage_info)) },
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ) {
                    //文件大小
                    Text(text = stringResource(R.string.generic_file_size, formatFileSize(mod.fileSize)))
                    //模组版本
                    mod.version?.let { version ->
                        Text(text = stringResource(R.string.mods_manage_version, version))
                    }
                    //作者
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = stringResource(R.string.mods_manage_authors))
                        FlowRow(
                            modifier = Modifier.weight(1f, fill = false),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            mod.authors.forEach { author ->
                                Text(text = author)
                            }
                        }
                    }
                    //模组描述
                    mod.description?.takeIf { it.isNotEmptyOrBlank() }?.let { description ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = stringResource(R.string.mods_manage_description))
                            Text(
                                modifier = Modifier.weight(1f, fill = false),
                                text = description
                            )
                        }
                    }
                }
            }
        }
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = stringResource(R.string.mods_manage_info)
        )
    }
}


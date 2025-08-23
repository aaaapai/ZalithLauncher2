package com.movtery.zalithlauncher.ui.screens.content.versions

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.ContentCheckBox
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.ProgressDialog
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.components.TooltipIconButton
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ByteArrayIcon
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.FileNameInputDialog
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.LoadingState
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.MinecraftColorTextNormal
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ResourcePackFilter
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ResourcePackInfo
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ResourcePackOperation
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.filterPacks
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.parseResourcePack
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.VersionSettingsBackground
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.file.formatFileSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

private class ResourcePackManageViewModel(
    val resourcePackDir: File
) : ViewModel() {
    var packFilter by mutableStateOf(ResourcePackFilter(false, ""))
        private set

    var allPacks by mutableStateOf<List<ResourcePackInfo>>(emptyList())
        private set
    var filteredPacks by mutableStateOf<List<ResourcePackInfo>?>(null)
        private set

    var packState by mutableStateOf<LoadingState>(LoadingState.None)
        private set

    fun refresh() {
        println("Test: refreshing")
        viewModelScope.launch {
            packState = LoadingState.Loading

            withContext(Dispatchers.IO) {
                val tempList = mutableListOf<ResourcePackInfo>()
                try {
                    resourcePackDir.listFiles()?.forEach { file ->
                        parseResourcePack(file)?.let {
                            ensureActive()
                            tempList.add(it)
                        }
                    }
                } catch (_: CancellationException) {
                    return@withContext
                }
                allPacks = tempList.sortedBy { it.rawName }
                filterPacks()
            }

            packState = LoadingState.None
        }
    }

    init {
        refresh()
    }

    fun updateFilter(filter: ResourcePackFilter) {
        this.packFilter = filter
        filterPacks()
    }

    private fun filterPacks() {
        filteredPacks = allPacks.takeIf { it.isNotEmpty() }?.filterPacks(packFilter)
    }
}

@Composable
private fun rememberResourcePackManageViewModel(
    resourcePackDir: File,
    version: Version
) = viewModel(
    key = version.toString() + "_" + VersionFolders.RESOURCE_PACK.folderName
) {
    ResourcePackManageViewModel(resourcePackDir)
}

@Composable
fun ResourcePackManageScreen(
    mainScreenKey: NavKey?,
    versionsScreenKey: NavKey?,
    version: Version,
    swapToDownload: () -> Unit = {}
) {
    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.Versions::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.ResourcePackManager, versionsScreenKey, false)
    ) { isVisible ->
        val resourcePackDir = File(version.getGameDir(), VersionFolders.RESOURCE_PACK.folderName)

        val viewModel = rememberResourcePackManageViewModel(resourcePackDir, version)

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
            val operationScope = rememberCoroutineScope()

            when (viewModel.packState) {
                is LoadingState.None -> {
                    val itemColor = itemLayoutColor()
                    val itemContentColor = MaterialTheme.colorScheme.onSurface

                    var resourcePackOperation by remember { mutableStateOf<ResourcePackOperation>(ResourcePackOperation.None) }
                    fun runProgress(task: () -> Unit) {
                        operationScope.launch(Dispatchers.IO) {
                            resourcePackOperation = ResourcePackOperation.Progress
                            task()
                            resourcePackOperation = ResourcePackOperation.None
                            viewModel.refresh()
                        }
                    }
                    ResourcePackOperation(
                        resourcePackDir = resourcePackDir,
                        resourcePackOperation = resourcePackOperation,
                        updateOperation = { resourcePackOperation = it },
                        onRename = { newName, packInfo ->
                            runProgress {
                                val extension = if (packInfo.file.isFile) {
                                    ".${packInfo.file.extension}"
                                } else ""
                                packInfo.file.renameTo(File(resourcePackDir, "$newName$extension"))
                            }
                        },
                        onDelete = { packInfo ->
                            runProgress {
                                FileUtils.deleteQuietly(packInfo.file)
                            }
                        }
                    )

                    Column {
                        ResourcePackHeader(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .padding(top = 4.dp)
                                .fillMaxWidth(),
                            inputFieldColor = itemColor,
                            inputFieldContentColor = itemContentColor,
                            packFilter = viewModel.packFilter,
                            changePackFilter = { viewModel.updateFilter(it) },
                            swapToDownload = swapToDownload,
                            onRefresh = {
                                viewModel.refresh()
                            }
                        )

                        ResourcePackList(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            packList = viewModel.filteredPacks,
                            itemColor = itemColor,
                            itemContentColor = itemContentColor,
                            updateOperation = { resourcePackOperation = it }
                        )
                    }
                }
                is LoadingState.Loading -> {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourcePackHeader(
    modifier: Modifier = Modifier,
    inputFieldColor: Color,
    inputFieldContentColor: Color,
    packFilter: ResourcePackFilter,
    changePackFilter: (ResourcePackFilter) -> Unit,
    swapToDownload: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            SimpleTextInputField(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                value = packFilter.filterName,
                onValueChange = { changePackFilter(packFilter.copy(filterName = it)) },
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

            ContentCheckBox(
                checked = packFilter.onlyShowValid,
                onCheckedChange = { changePackFilter(packFilter.copy(onlyShowValid = it)) }
            ) {
                Text(
                    text = stringResource(R.string.manage_only_show_valid),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconTextButton(
                onClick = swapToDownload,
                imageVector = Icons.Default.Download,
                text = stringResource(R.string.generic_download)
            )

            IconButton(
                onClick = onRefresh
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
private fun ResourcePackList(
    modifier: Modifier = Modifier,
    packList: List<ResourcePackInfo>?,
    itemColor: Color,
    itemContentColor: Color,
    updateOperation: (ResourcePackOperation) -> Unit
) {
    packList?.let { list ->
        //如果列表是空的，则是由搜索导致的
        if (list.isNotEmpty()) {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                items(list) { pack ->
                    ResourcePackItemLayout(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        resourcePackInfo = pack,
                        itemColor = itemColor,
                        itemContentColor = itemContentColor,
                        updateOperation = updateOperation
                    )
                }
            }
        }
    } ?: run {
        //如果为null，则代表本身就没有资源包可以展示
        Box(modifier = Modifier.fillMaxSize()) {
            ScalingLabel(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(R.string.resource_pack_manage_no_packs)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourcePackItemLayout(
    modifier: Modifier = Modifier,
    resourcePackInfo: ResourcePackInfo,
    onClick: () -> Unit = {},
    itemColor: Color,
    itemContentColor: Color,
    shadowElevation: Dp = 1.dp,
    updateOperation: (ResourcePackOperation) -> Unit
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
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
            ByteArrayIcon(
                modifier = Modifier
                    .size(48.dp)
                    .clip(shape = RoundedCornerShape(10.dp)),
                triggerRefresh = resourcePackInfo,
                icon = resourcePackInfo.icon
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                MinecraftColorTextNormal(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    inputText = resourcePackInfo.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                resourcePackInfo.description?.let { description ->
                    MinecraftColorTextNormal(
                        inputText = description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (resourcePackInfo.isValid) {
                    //详细信息展示
                    TooltipIconButton(
                        modifier = Modifier.size(38.dp),
                        tooltip = {
                            RichTooltip(
                                modifier = Modifier.padding(all = 3.dp),
                                title = { Text(text = stringResource(R.string.resource_pack_manage_info)) },
                                shadowElevation = 3.dp
                            ) {
                                ResourcePackInfoTooltip(resourcePackInfo)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.saves_manage_info)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.resource_pack_manage_invalid),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                //更多操作
                ResourcePackOperationMenu(
                    resourcePackInfo = resourcePackInfo,
                    buttonSize = 38.dp,
                    iconSize = 26.dp,
                    onRenameClick = {
                        updateOperation(ResourcePackOperation.RenamePack(resourcePackInfo))
                    },
                    onDeleteClick = {
                        updateOperation(ResourcePackOperation.DeletePack(resourcePackInfo))
                    }
                )
            }
        }
    }
}

@Composable
private fun ResourcePackOperationMenu(
    resourcePackInfo: ResourcePackInfo,
    buttonSize: Dp,
    iconSize: Dp = buttonSize,
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Row {
        var menuExpanded by remember { mutableStateOf(false) }

        IconButton(
            modifier = Modifier.size(buttonSize),
            onClick = { menuExpanded = !menuExpanded }
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = stringResource(R.string.generic_more)
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 3.dp,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                enabled = resourcePackInfo.isValid,
                text = {
                    Text(text = stringResource(R.string.generic_rename))
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.generic_rename)
                    )
                },
                onClick = {
                    onRenameClick()
                    menuExpanded = false
                }
            )
            DropdownMenuItem(
                text = {
                    Text(text = stringResource(R.string.generic_delete))
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.generic_delete)
                    )
                },
                onClick = {
                    onDeleteClick()
                    menuExpanded = false
                }
            )
        }
    }
}

@Composable
private fun ResourcePackOperation(
    resourcePackDir: File,
    resourcePackOperation: ResourcePackOperation,
    updateOperation: (ResourcePackOperation) -> Unit,
    onRename: (String, ResourcePackInfo) -> Unit,
    onDelete: (ResourcePackInfo) -> Unit
) {
    when (resourcePackOperation) {
        is ResourcePackOperation.None -> {}
        is ResourcePackOperation.Progress -> {
            ProgressDialog()
        }
        is ResourcePackOperation.RenamePack -> {
            val packInfo = resourcePackOperation.packInfo
            FileNameInputDialog(
                initValue = packInfo.displayName,
                existsCheck = { value ->
                    val fileName = if (packInfo.file.isDirectory) {
                        value //文件夹类型，不做扩展名处理
                    } else {
                        "$value.${packInfo.file.extension}"
                    }

                    if (File(resourcePackDir, fileName).exists()) {
                        stringResource(R.string.resource_pack_manage_exists)
                    } else {
                        null
                    }
                },
                title = stringResource(R.string.generic_rename),
                label = stringResource(R.string.resource_pack_manage_name),
                onDismissRequest = {
                    updateOperation(ResourcePackOperation.None)
                },
                onConfirm = { value ->
                    onRename(value, packInfo)
                }
            )
        }
        is ResourcePackOperation.DeletePack -> {
            val packInfo = resourcePackOperation.packInfo
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.resource_pack_manage_delete_warning, packInfo.file.name),
                onDismiss = {
                    updateOperation(ResourcePackOperation.None)
                },
                onConfirm = {
                    onDelete(packInfo)
                    updateOperation(ResourcePackOperation.None)
                }
            )
        }
    }
}

@Composable
private fun ResourcePackInfoTooltip(
    resourcePackInfo: ResourcePackInfo
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(
                R.string.resource_pack_manage_type,
                if (resourcePackInfo.file.isDirectory) {
                    stringResource(R.string.resource_pack_manage_type_folder)
                } else {
                    stringResource(R.string.resource_pack_manage_type_zip)
                }
            )
        )
        Text(text = stringResource(R.string.generic_file_name, resourcePackInfo.file.name))
        Text(text = stringResource(R.string.generic_file_size, formatFileSize(resourcePackInfo.fileSize)))
        resourcePackInfo.packFormat?.let { packFormat ->
            Text(text = stringResource(R.string.resource_pack_manage_formats, packFormat.toString()))
        }
    }
}
package com.movtery.zalithlauncher.ui.screens.content.versions

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.state.ObjectStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.ContentCheckBox
import com.movtery.zalithlauncher.ui.components.ProgressDialog
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.components.TooltipIconButton
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.content.VERSION_SETTINGS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.FileNameInputDialog
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.MinecraftColorTextNormal
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ResourcePackFilter
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ResourcePackInfo
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ResourcePackOperation
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ResourcePackState
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

const val RESOURCE_PACK_MANAGE_SCREEN_TAG = "ResourcePackManageScreen"

@Composable
fun ResourcePackManageScreen() {
    BaseScreen(
        parentScreenTag = VERSION_SETTINGS_SCREEN_TAG,
        parentCurrentTag = MutableStates.mainScreenTag,
        childScreenTag = RESOURCE_PACK_MANAGE_SCREEN_TAG,
        childCurrentTag = MutableStates.versionSettingsScreenTag
    ) { isVisible ->

        val version = VersionsManager.versionBeingSet?.takeIf { it.isValid() } ?: run {
            ObjectStates.backToLauncherScreen()
            return@BaseScreen
        }
        val resourcePackDir = File(version.getGameDir(), "resourcepacks")

        //触发刷新
        var refreshTrigger by remember { mutableStateOf(false) }
        //简易名称过滤器
        var packFilter by remember { mutableStateOf(ResourcePackFilter(false, "")) }

        var allPacks by remember { mutableStateOf<List<ResourcePackInfo>>(emptyList()) }
        val filteredPacks by remember(allPacks, packFilter) {
            derivedStateOf {
                allPacks.takeIf { it.isNotEmpty() }?.filterPacks(packFilter)
            }
        }

        var packState by remember { mutableStateOf<ResourcePackState>(ResourcePackState.None) }

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

            when (packState) {
                is ResourcePackState.None -> {
                    val itemColor = itemLayoutColor()
                    val itemContentColor = MaterialTheme.colorScheme.onSurface

                    var resourcePackOperation by remember { mutableStateOf<ResourcePackOperation>(ResourcePackOperation.None) }
                    fun runProgress(task: () -> Unit) {
                        operationScope.launch(Dispatchers.IO) {
                            resourcePackOperation = ResourcePackOperation.Progress
                            task()
                            resourcePackOperation = ResourcePackOperation.None
                            refreshTrigger = !refreshTrigger
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
                            packFilter = packFilter,
                            changePackFilter = { packFilter = it },
                            onRefresh = {
                                refreshTrigger = !refreshTrigger
                            }
                        )

                        ResourcePackList(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            packList = filteredPacks,
                            itemColor = itemColor,
                            itemContentColor = itemContentColor,
                            updateOperation = { resourcePackOperation = it }
                        )
                    }
                }
                is ResourcePackState.Loading -> {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }
            }
        }

        LaunchedEffect(refreshTrigger) {
            packState = ResourcePackState.Loading

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
            }

            packState = ResourcePackState.None
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
    onRefresh: () -> Unit = {}
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ContentCheckBox(
                checked = packFilter.onlyShowValid,
                onCheckedChange = { changePackFilter(packFilter.copy(onlyShowValid = it)) }
            ) {
                Text(
                    text = stringResource(R.string.resource_pack_manage_only_show_valid),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SimpleTextInputField(
                    modifier = Modifier.weight(1f),
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

                IconButton(
                    onClick = onRefresh
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.generic_refresh)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
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
            ResourcePackIcon(
                modifier = Modifier
                    .size(48.dp)
                    .clip(shape = RoundedCornerShape(10.dp)),
                resourcePackInfo = resourcePackInfo
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                MinecraftColorTextNormal(
                    inputText = resourcePackInfo.displayName,
                    style = MaterialTheme.typography.titleSmall
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
private fun ResourcePackIcon(
    modifier: Modifier = Modifier,
    triggerRefresh: Any? = null,
    resourcePackInfo: ResourcePackInfo
) {
    val context = LocalContext.current

    val imageLoader = remember(triggerRefresh, context) {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }

    val (model, defaultRes) = remember(triggerRefresh, context) {
        val default = null to R.drawable.ic_unknown_icon
        val icon = resourcePackInfo.icon
        when {
            icon == null -> default //不存在则使用默认
            else -> {
                val model = ImageRequest.Builder(context)
                    .data(icon)
                    .build()
                model to null
            }
        }
    }

    if (model != null) {
        AsyncImage(
            model = model,
            imageLoader = imageLoader,
            contentDescription = null,
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = defaultRes!!),
            contentDescription = null,
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
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
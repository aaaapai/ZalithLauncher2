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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
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
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.ProgressDialog
import com.movtery.zalithlauncher.ui.components.ScalingLabel
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.FileNameInputDialog
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.LoadingState
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ShaderOperation
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.ShaderPackInfo
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.filterShaders
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

private class ShadersManageViewModel(
    val shadersDir: File
) : ViewModel() {
    var nameFilter by mutableStateOf("")

    var allShaders by mutableStateOf<List<ShaderPackInfo>>(emptyList())
        private set
    var filteredShaders by mutableStateOf<List<ShaderPackInfo>?>(null)
        private set

    var shadersState by mutableStateOf<LoadingState>(LoadingState.None)
        private set

    fun refresh() {
        viewModelScope.launch {
            shadersState = LoadingState.Loading

            withContext(Dispatchers.IO) {
                try {
                    val list = shadersDir.listFiles()?.filter {
                        //光影包只能是后缀为.zip的压缩包
                        it.isFile && it.extension.equals("zip", true)
                    }?.map { file ->
                        ensureActive()
                        ShaderPackInfo(
                            file = file,
                            fileSize = FileUtils.sizeOf(file)
                        )
                    } ?: emptyList()
                    allShaders = list.sortedBy { it.file.name }
                    filterShaders()
                } catch (_: CancellationException) {
                    return@withContext
                }
            }

            shadersState = LoadingState.None
        }
    }

    init {
        refresh()
    }

    fun updateFilter(name: String) {
        this.nameFilter = name
        filterShaders()
    }

    private fun filterShaders() {
        filteredShaders = allShaders.takeIf { it.isNotEmpty() }?.filterShaders(nameFilter)
    }
}

@Composable
private fun rememberShadersManageViewModel(
    shadersDir: File,
    version: Version
) = viewModel(
    key = version.toString() + "_" + VersionFolders.SHADERS.folderName
) {
    ShadersManageViewModel(shadersDir)
}

@Composable
fun ShadersManagerScreen(
    mainScreenKey: NavKey?,
    versionsScreenKey: NavKey?,
    version: Version,
    swapToDownload: () -> Unit = {}
) {
    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.Versions::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.ShadersManager, versionsScreenKey, false),
    ) { isVisible ->
        val shadersDir = File(version.getGameDir(), VersionFolders.SHADERS.folderName)

        val viewModel = rememberShadersManageViewModel(shadersDir, version)

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

            when (viewModel.shadersState) {
                LoadingState.None -> {
                    val itemColor = itemLayoutColor()
                    val itemContentColor = MaterialTheme.colorScheme.onSurface

                    var shaderOperation by remember { mutableStateOf<ShaderOperation>(ShaderOperation.None) }
                    fun runProgress(task: () -> Unit) {
                        operationScope.launch(Dispatchers.IO) {
                            shaderOperation = ShaderOperation.Progress
                            task()
                            shaderOperation = ShaderOperation.None
                            viewModel.refresh()
                        }
                    }
                    ShaderOperation(
                        shaderOperation = shaderOperation,
                        updateOperation = { shaderOperation = it },
                        shadersDir = shadersDir,
                        renameShaderPack = { info, newName ->
                            runProgress {
                                val file = info.file
                                file.renameTo(File(shadersDir, "$newName.${file.extension}"))
                            }
                        },
                        deleteShaderPack = { info ->
                            runProgress {
                                FileUtils.deleteQuietly(info.file)
                            }
                        }
                    )

                    Column {
                        ShadersActionsHeader(
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

                        ShadersList(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shadersList = viewModel.filteredShaders,
                            itemColor = itemColor,
                            itemContentColor = itemContentColor,
                            updateOperation = { shaderOperation = it }
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
private fun ShadersActionsHeader(
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
private fun ShadersList(
    modifier: Modifier = Modifier,
    shadersList: List<ShaderPackInfo>?,
    itemColor: Color,
    itemContentColor: Color,
    updateOperation: (ShaderOperation) -> Unit
) {
    shadersList?.let { list ->
        //如果列表是空的，则是由搜索导致的
        if (list.isNotEmpty()) {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                items(list) { info ->
                    ShaderPackItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shaderPackInfo = info,
                        updateOperation = updateOperation,
                        itemColor = itemColor,
                        itemContentColor = itemContentColor
                    )
                }
            }
        }
    } ?: run {
        //如果为null，则代表本身就没有光影包可以展示
        Box(modifier = Modifier.fillMaxSize()) {
            ScalingLabel(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(R.string.shader_pack_manage_no_packs)
            )
        }
    }
}

@Composable
private fun ShaderPackItem(
    modifier: Modifier = Modifier,
    shaderPackInfo: ShaderPackInfo,
    onClick: () -> Unit = {},
    updateOperation: (ShaderOperation) -> Unit,
    itemColor: Color,
    itemContentColor: Color,
    shadowElevation: Dp = 1.dp
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
            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f),
            ) {
                //文件名称
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    text = shaderPackInfo.file.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                //文件大小
                Text(
                    modifier = Modifier.alpha(0.7f),
                    text = stringResource(
                        R.string.generic_file_size,
                        formatFileSize(shaderPackInfo.fileSize)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShadersOperationMenu(
                    buttonSize = 38.dp,
                    iconSize = 26.dp,
                    onRenameClick = {
                        updateOperation(ShaderOperation.Rename(shaderPackInfo))
                    },
                    onDeleteClick = {
                        updateOperation(ShaderOperation.Delete(shaderPackInfo))
                    }
                )
            }
        }
    }
}

@Composable
private fun ShadersOperationMenu(
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
                text = { Text(text = stringResource(R.string.generic_rename)) },
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
                text = { Text(text = stringResource(R.string.generic_delete)) },
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
private fun ShaderOperation(
    shaderOperation: ShaderOperation,
    updateOperation: (ShaderOperation) -> Unit,
    shadersDir: File,
    renameShaderPack: (ShaderPackInfo, String) -> Unit,
    deleteShaderPack: (ShaderPackInfo) -> Unit
) {
    when (shaderOperation) {
        is ShaderOperation.None -> {}
        is ShaderOperation.Progress -> {
            ProgressDialog()
        }
        is ShaderOperation.Rename -> {
            val info = shaderOperation.info
            FileNameInputDialog(
                initValue = info.file.nameWithoutExtension,
                existsCheck = { value ->
                    if (File(shadersDir, "$value.${info.file.extension}").exists()) {
                        stringResource(R.string.shader_pack_manage_exists)
                    } else {
                        null
                    }
                },
                title = stringResource(R.string.generic_rename),
                label = stringResource(R.string.shader_pack_manage_name),
                onDismissRequest = {
                    updateOperation(ShaderOperation.None)
                },
                onConfirm = { newName ->
                    renameShaderPack(info, newName)
                    updateOperation(ShaderOperation.None)
                }
            )
        }
        is ShaderOperation.Delete -> {
            val info = shaderOperation.info
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.shader_pack_manage_delete_warning, info.file.name),
                onDismiss = {
                    updateOperation(ShaderOperation.None)
                },
                onConfirm = {
                    deleteShaderPack(info)
                    updateOperation(ShaderOperation.None)
                }
            )
        }
    }
}
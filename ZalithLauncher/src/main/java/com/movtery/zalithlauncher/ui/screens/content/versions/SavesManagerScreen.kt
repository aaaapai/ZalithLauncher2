package com.movtery.zalithlauncher.ui.screens.content.versions

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.CopyAll
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
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.ui.components.SimpleTextInputField
import com.movtery.zalithlauncher.ui.components.TooltipIconButton
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.content.VERSION_SETTINGS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.elements.isFilenameInvalid
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.SaveData
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.SavesFilter
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.SavesOperation
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.SavesState
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.filterSaves
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.isCompatible
import com.movtery.zalithlauncher.ui.screens.content.versions.elements.parseLevelDatFile
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.VersionSettingsBackground
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.copyText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File

const val SAVES_MANAGER_SCREEN_TAG = "SavesManagerScreen"

@Composable
fun SavesManagerScreen() {
    BaseScreen(
        parentScreenTag = VERSION_SETTINGS_SCREEN_TAG,
        parentCurrentTag = MutableStates.mainScreenTag,
        childScreenTag = SAVES_MANAGER_SCREEN_TAG,
        childCurrentTag = MutableStates.versionSettingsScreenTag
    ) { isVisible ->

        val version = VersionsManager.versionBeingSet?.takeIf { it.isValid() } ?: run {
            ObjectStates.backToLauncherScreen()
            return@BaseScreen
        }
        val minecraftVersion = version.getVersionInfo()!!.minecraftVersion
        val savesDir = File(version.getGameDir(), "saves")

        //触发刷新
        var refreshTrigger by remember { mutableStateOf(false) }
        //简易存档过滤器
        var savesFilter by remember { mutableStateOf(SavesFilter(onlyShowCompatible = false, saveName = "")) }

        var allSaves by remember { mutableStateOf<List<SaveData>>(emptyList()) }
        var filteredSaves by remember { mutableStateOf<List<SaveData>>(emptyList()) }

        var savesState by remember { mutableStateOf<SavesState>(SavesState.Loading) }

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

            when (savesState) {
                is SavesState.None -> {
                    val itemColor = itemLayoutColor()
                    val itemContentColor = MaterialTheme.colorScheme.onSurface

                    var savesOperation by remember { mutableStateOf<SavesOperation>(SavesOperation.None) }
                    SaveOperation(
                        savesOperation = savesOperation,
                        savesDir = savesDir,
                        updateOperation = { savesOperation = it },
                        renameSave = { saveData, newName ->
                            operationScope.launch(Dispatchers.IO) {
                                saveData.saveFile.renameTo(File(savesDir, newName))
                                refreshTrigger = !refreshTrigger
                            }
                        },
                        backupSave = { saveData, name ->
                            operationScope.launch(Dispatchers.IO) {
                                FileUtils.copyDirectory(saveData.saveFile, File(savesDir, name))
                                refreshTrigger = !refreshTrigger
                            }
                        },
                        deleteSave = { saveData ->
                            operationScope.launch(Dispatchers.IO) {
                                FileUtils.deleteQuietly(saveData.saveFile)
                                refreshTrigger = !refreshTrigger
                            }
                        }
                    )

                    Column {
                        SavesActionsHeader(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .padding(top = 4.dp)
                                .fillMaxWidth(),
                            inputFieldColor = itemColor,
                            inputFieldContentColor = itemContentColor,
                            savesFilter = savesFilter,
                            onSavesFilterChange = { savesFilter = it },
                            refreshSaves = { refreshTrigger = !refreshTrigger }
                        )

                        SavesList(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            savesList = filteredSaves,
                            minecraftVersion = minecraftVersion,
                            itemColor = itemColor,
                            itemContentColor = itemContentColor,
                            updateOperation = { savesOperation = it }
                        )
                    }
                }
                is SavesState.Loading -> {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }
            }
        }

        LaunchedEffect(refreshTrigger) {
            savesState = SavesState.Loading

            withContext(Dispatchers.IO) {
                val tempList = mutableListOf<SaveData>()
                savesDir.listFiles()?.filter { it.isDirectory }?.takeIf { it.isNotEmpty() }?.let { dirs ->
                    try {
                        dirs.forEach { dir ->
                            ensureActive()

                            val levelDatFile = File(dir, "level.dat").takeIf { it.exists() && it.isFile } ?: return@forEach
                            //解析存档 level.dat，读取必要数据
                            val data = parseLevelDatFile(
                                saveFile = dir,
                                levelDatFile = levelDatFile
                            )
                            tempList.add(data)
                        }
                    } catch (_: CancellationException) {
                        return@withContext
                    }
                }
                allSaves = tempList.sortedBy { it.saveFile.name }
                filteredSaves = withContext(Dispatchers.Default) {
                    allSaves.filterSaves(minecraftVersion, savesFilter)
                }
            }

            savesState = SavesState.None
        }

        LaunchedEffect(savesFilter) {
            if (allSaves.isNotEmpty()) {
                filteredSaves = withContext(Dispatchers.Default) {
                    allSaves.filterSaves(minecraftVersion, savesFilter)
                }
            }
        }
    }
}

@Composable
private fun SavesActionsHeader(
    modifier: Modifier,
    inputFieldColor: Color,
    inputFieldContentColor: Color,
    savesFilter: SavesFilter,
    onSavesFilterChange: (SavesFilter) -> Unit = {},
    refreshSaves: () -> Unit = {}
) {
    Column(modifier = modifier) {
        Row {
            ContentCheckBox(
                checked = savesFilter.onlyShowCompatible,
                onCheckedChange = { onSavesFilterChange(savesFilter.copy(onlyShowCompatible = it)) }
            ) {
                Text(
                    text = stringResource(R.string.saves_manage_only_show_compatible),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SimpleTextInputField(
                    modifier = Modifier.weight(1f),
                    value = savesFilter.saveName,
                    onValueChange = { onSavesFilterChange(savesFilter.copy(saveName = it)) },
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

                IconButton(
                    onClick = refreshSaves
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

/**
 * @param minecraftVersion 当前版本的 Minecraft 版本
 */
@Composable
private fun SavesList(
    modifier: Modifier = Modifier,
    savesList: List<SaveData>,
    minecraftVersion: String,
    itemColor: Color,
    itemContentColor: Color,
    updateOperation: (SavesOperation) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(all = 12.dp)
    ) {
        items(savesList.size) { index ->
            val saveData = savesList[index]
            SaveItemLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (index != savesList.size - 1) 12.dp else 0.dp),
                saveData = saveData,
                minecraftVersion = minecraftVersion,
                updateOperation = updateOperation,
                itemColor = itemColor,
                itemContentColor = itemContentColor
            )
        }
    }
}

/**
 * @param saveData 存档信息
 * @param minecraftVersion 当前版本的 Minecraft 版本
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SaveItemLayout(
    modifier: Modifier = Modifier,
    saveData: SaveData,
    minecraftVersion: String,
    onClick: () -> Unit = {},
    updateOperation: (SavesOperation) -> Unit = {},
    itemColor: Color,
    itemContentColor: Color,
    shadowElevation: Dp = 1.dp
) {
    //存档是否与当前 MC 版本兼容
    val isCompatible = saveData.isCompatible(minecraftVersion)

    val context = LocalContext.current

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
        Row(modifier = Modifier.padding(all = 8.dp)) {
            //存档的封面图标
            SaveIcon(
                modifier = Modifier
                    .size(38.dp)
                    .clip(shape = RoundedCornerShape(10.dp)),
                saveData = saveData
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = saveData.saveFile.name,
                    style = MaterialTheme.typography.titleSmall
                )
                FlowRow {
                    if (!saveData.isValid) {
                        Text(
                            text = stringResource(R.string.saves_manage_invalid),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        saveData.levelName?.takeIf { it.isNotEmpty() }?.let { levelName ->
                            Text(
                                text = stringResource(R.string.saves_manage_level_name, levelName),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        saveData.levelMCVersion?.takeIf { it.isNotEmpty() }?.let { levelMCVer ->
                            Spacer(modifier = Modifier.width(16.dp))
                            if (isCompatible) {
                                Text(
                                    text = levelMCVer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = levelMCVer,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        //虽然极限模式与 gameMode 是分离开的
                        //不过它可以算作是一种游戏模式，毕竟创建世界时，极限模式就是在游戏模式里面选择的
                        if (saveData.hardcoreMode == true) {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.saves_manage_hardcore),
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            saveData.gameMode?.let { gameMode ->
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(gameMode.nameRes),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                if (saveData.isValid) {
                    //详细信息展示
                    TooltipIconButton(
                        modifier = Modifier.size(38.dp),
                        tooltip = {
                            RichTooltip(
                                modifier = Modifier.padding(all = 3.dp),
                                title = { Text(text = stringResource(R.string.saves_manage_info)) },
                                shadowElevation = 3.dp
                            ) {
                                SaveInfoTooltip(saveData) { seed ->
                                    copyText(null, seed, context)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.saves_manage_info)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                //更多存档操作
                SaveOperationMenu(
                    buttonSize = 38.dp,
                    iconSize = 26.dp,
                    onRenameClick = {
                        updateOperation(SavesOperation.RenameSave(saveData))
                    },
                    onBackupClick = {
                        updateOperation(SavesOperation.BackupSave(saveData))
                    },
                    onDeleteClick = {
                        updateOperation(SavesOperation.DeleteSave(saveData))
                    }
                )
            }
        }
    }
}

/**
 * 存档的封面图标
 * @param triggerRefresh 强制刷新
 */
@Composable
private fun SaveIcon(
    modifier: Modifier = Modifier,
    saveData: SaveData,
    triggerRefresh: Any? = null
) {
    val context = LocalContext.current
    val iconFile = File(saveData.saveFile, "icon.png")

    val imageLoader = remember(triggerRefresh, context) {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }

    val (model, defaultRes) = remember(triggerRefresh, context) {
        val default = null to R.drawable.ic_unknown_icon
        val file = iconFile.takeIf { it.exists() && it.isFile }
        when {
            file == null -> default //不存在则使用默认
            else -> {
                val model = ImageRequest.Builder(context)
                    .data(file)
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
private fun SaveInfoTooltip(
    saveData: SaveData,
    copySeed: (String) -> Unit = {}
) {
    Column {
        //存档名，不存在则不展示
        saveData.levelName?.takeIf { it.isNotEmpty() }?.let { levelName ->
            Text(text = stringResource(R.string.saves_manage_level_name, levelName))
        }
        //游戏模式，不存在则展示为未知
        Text(
            text = stringResource(
                R.string.saves_manage_gamemode,
                stringResource(
                    if (saveData.hardcoreMode == true) {
                        //极限模式
                        R.string.saves_manage_hardcore
                    } else {
                        saveData.gameMode?.nameRes ?: R.string.generic_unknown
                    }
                )
            )
        )
        //游戏难度
        Row {
            //游戏难度，不存在则展示为未知
            Text(
                text = stringResource(
                    R.string.saves_manage_difficulty,
                    stringResource(saveData.difficulty?.nameRes ?: R.string.generic_unknown)
                )
            )
            if (saveData.difficultyLocked == true) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.saves_manage_difficulty_locked))
            }
        }
        //是否使用指令
        if (saveData.allowCommands == true) {
            Text(text = stringResource(R.string.saves_manage_allow_commands))
        }
        //世界种子
        val worldSeed = saveData.worldSeed?.toString()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                    R.string.saves_manage_world_seed,
                    worldSeed ?: stringResource(R.string.generic_unknown)
                )
            )
            //不为未知时，允许复制种子码
            worldSeed?.let { seed ->
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = {
                        copySeed(seed)
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(18.dp),
                        imageVector = Icons.Outlined.CopyAll,
                        contentDescription = stringResource(R.string.generic_copy)
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveOperationMenu(
    buttonSize: Dp,
    iconSize: Dp = buttonSize,
    onRenameClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
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
                text = { Text(text = stringResource(R.string.saves_manage_backup)) },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Filled.Save,
                        contentDescription = stringResource(R.string.saves_manage_backup)
                    )
                },
                onClick = {
                    onBackupClick()
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
private fun SaveOperation(
    savesOperation: SavesOperation,
    savesDir: File,
    updateOperation: (SavesOperation) -> Unit,
    renameSave: (SaveData, String) -> Unit,
    backupSave: (SaveData, String) -> Unit,
    deleteSave: (SaveData) -> Unit
) {
    when (savesOperation) {
        is SavesOperation.None -> {}
        is SavesOperation.RenameSave -> {
            val saveData = savesOperation.saveData
            SaveNameInputDialog(
                saveData = saveData,
                savesDir = savesDir,
                title = stringResource(R.string.generic_rename),
                onDismissRequest = {
                    updateOperation(SavesOperation.None)
                },
                onConfirm = { value ->
                    renameSave(saveData, value)
                    updateOperation(SavesOperation.None)
                }
            )
        }
        is SavesOperation.BackupSave -> {
            val saveData = savesOperation.saveData
            SaveNameInputDialog(
                saveData = saveData,
                savesDir = savesDir,
                title = stringResource(R.string.saves_manage_backup),
                onDismissRequest = {
                    updateOperation(SavesOperation.None)
                },
                onConfirm = { value ->
                    backupSave(saveData, value)
                    updateOperation(SavesOperation.None)
                }
            )
        }
        is SavesOperation.DeleteSave -> {
            val saveData = savesOperation.saveData
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.saves_manage_delete_warning, saveData.saveFile.name),
                onDismiss = {
                    updateOperation(SavesOperation.None)
                },
                onConfirm = {
                    deleteSave(saveData)
                    updateOperation(SavesOperation.None)
                }
            )
        }
    }
}

@Composable
private fun SaveNameInputDialog(
    saveData: SaveData,
    savesDir: File,
    title: String,
    onDismissRequest: () -> Unit = {},
    onConfirm: (vale: String) -> Unit = {}
) {
    var value by remember { mutableStateOf(saveData.saveFile.name) }
    var errorMessage by remember { mutableStateOf("") }

    val isError = value.isEmpty() || isFilenameInvalid(value) { message ->
        errorMessage = message
    } || File(savesDir, value).exists().also {
        if (it) errorMessage = stringResource(R.string.saves_manage_exists)
    }

    SimpleEditDialog(
        title = title,
        value = value,
        onValueChange = { value = it },
        isError = isError,
        label = {
            Text(text = stringResource(R.string.saves_manage_save_name))
        },
        supportingText = {
            when {
                value.isEmpty() -> Text(text = stringResource(R.string.generic_cannot_empty))
                isError -> Text(text = errorMessage)
            }
        },
        singleLine = true,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            if (!isError) {
                onConfirm(value)
            }
        }
    )
}
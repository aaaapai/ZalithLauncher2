package com.movtery.zalithlauncher.ui.screens.content.elements

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.path.GamePathItem
import com.movtery.zalithlauncher.game.path.GamePathManager
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.state.ObjectStates
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleCheckEditDialog
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.ui.components.SimpleTaskDialog
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.components.secondaryContainerDrawerItemColors
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.string.StringUtils
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.getMessageOrToString
import kotlinx.coroutines.Dispatchers

sealed interface GamePathOperation {
    data object None: GamePathOperation
    data object PathExists: GamePathOperation
    data class AddNewPath(val path: String): GamePathOperation
    data class RenamePath(val item: GamePathItem): GamePathOperation
    data class DeletePath(val item: GamePathItem): GamePathOperation
}

sealed interface VersionsOperation {
    data object None: VersionsOperation
    data class Rename(val version: Version): VersionsOperation
    data class Copy(val version: Version): VersionsOperation
    data class Delete(val version: Version, val text: String? = null): VersionsOperation
    data class InvalidDelete(val version: Version): VersionsOperation
    data class RunTask(val title: Int, val task: suspend () -> Unit): VersionsOperation
}

@Composable
fun GamePathItemLayout(
    item: GamePathItem,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val notDefault = item.id != GamePathManager.DEFAULT_ID

    NavigationDrawerItem(
        modifier = modifier,
        colors = secondaryContainerDrawerItemColors(),
        label = {
            Column(
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    overflow = TextOverflow.Clip,
                    text = if (notDefault) item.title else stringResource(R.string.versions_manage_game_path_default),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    overflow = TextOverflow.Clip,
                    text = item.path,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        },
        badge = {
            var menuExpanded by remember { mutableStateOf(false) }

            IconButton(
                modifier = Modifier.size(24.dp),
                onClick = { menuExpanded = !menuExpanded }
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = stringResource(R.string.generic_more),
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                shape = MaterialTheme.shapes.large,
                shadowElevation = 3.dp,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    enabled = notDefault,
                    text = { Text(text = stringResource(R.string.generic_rename)) },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.generic_rename)
                        )
                    },
                    onClick = {
                        onRename()
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    enabled = notDefault,
                    text = { Text(text = stringResource(R.string.generic_delete)) },
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.generic_delete)
                        )
                    },
                    onClick = {
                        onDelete()
                        menuExpanded = false
                    }
                )
            }
        },
        selected = selected,
        onClick = onClick
    )
}

@Composable
fun GamePathOperation(
    gamePathOperation: GamePathOperation,
    changeState: (GamePathOperation) -> Unit
) {
    runCatching {
        when(gamePathOperation) {
            is GamePathOperation.None -> {}
            is GamePathOperation.AddNewPath -> {
                NameEditPathDialog(
                    onDismissRequest = { changeState(GamePathOperation.None) },
                    onConfirm = { value ->
                        if (GamePathManager.containsPath(gamePathOperation.path)) {
                            changeState(GamePathOperation.PathExists)
                        } else {
                            GamePathManager.addNewPath(title = value, path = gamePathOperation.path)
                            changeState(GamePathOperation.None)
                        }
                    }
                )
            }
            is GamePathOperation.RenamePath -> {
                NameEditPathDialog(
                    initValue = gamePathOperation.item.title,
                    onDismissRequest = { changeState(GamePathOperation.None) },
                    onConfirm = { value ->
                        GamePathManager.modifyTitle(gamePathOperation.item.id, value)
                        changeState(GamePathOperation.None)
                    }
                )
            }
            is GamePathOperation.DeletePath -> {
                SimpleAlertDialog(
                    title = stringResource(R.string.versions_manage_game_path_delete_title),
                    text = stringResource(R.string.versions_manage_game_path_delete_message),
                    onDismiss = { changeState(GamePathOperation.None) },
                    onConfirm = {
                        GamePathManager.removePath(gamePathOperation.item.id)
                        changeState(GamePathOperation.None)
                    }
                )
            }
            is GamePathOperation.PathExists -> {
                SimpleAlertDialog(
                    title = stringResource(R.string.versions_manage_game_path_exists_title),
                    text = stringResource(R.string.versions_manage_game_path_exists_message),
                    onDismiss = { changeState(GamePathOperation.None) }
                )
            }
        }
    }.onFailure { e ->
        ObjectStates.updateThrowable(
            ObjectStates.ThrowableMessage(
                title = stringResource(R.string.versions_manage_game_path_error_title),
                message = e.getMessageOrToString()
            )
        )
    }
}

@Composable
private fun NameEditPathDialog(
    initValue: String = "",
    onDismissRequest: () -> Unit = {},
    onConfirm: (value: String) -> Unit = {}
) {
    var value by remember { mutableStateOf(initValue) }
    SimpleEditDialog(
        title = stringResource(R.string.versions_manage_game_path_add_new),
        value = value,
        onValueChange = { value = it },
        label = { Text(text = stringResource(R.string.versions_manage_game_path_edit_title)) },
        isError = value.isEmpty(),
        supportingText = {
            if (value.isEmpty()) Text(text = stringResource(R.string.generic_cannot_empty))
        },
        singleLine = true,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            if (value.isNotEmpty() || value.isNotBlank()) {
                onConfirm(value.trim())
            }
        }
    )
}

@Composable
fun VersionsOperation(
    versionsOperation: VersionsOperation,
    updateVersionsOperation: (VersionsOperation) -> Unit
) {
    when(versionsOperation) {
        is VersionsOperation.None -> {}
        is VersionsOperation.Rename -> {
            RenameVersionDialog(
                version = versionsOperation.version,
                onDismissRequest = { updateVersionsOperation(VersionsOperation.None) },
                onConfirm = {
                    updateVersionsOperation(
                        VersionsOperation.RunTask(
                            title = R.string.versions_manage_rename_version,
                            task = {
                                VersionsManager.renameVersion(versionsOperation.version, it)
                                VersionsManager.refresh()
                            }
                        )
                    )
                }
            )
        }
        is VersionsOperation.Copy -> {
            CopyVersionDialog(
                version = versionsOperation.version,
                onDismissRequest = { updateVersionsOperation(VersionsOperation.None) },
                onConfirm = { name, copyAll ->
                    updateVersionsOperation(
                        VersionsOperation.RunTask(
                            title = R.string.versions_manage_copy_version,
                            task = { VersionsManager.copyVersion(versionsOperation.version, name, copyAll) }
                        )
                    )
                }
            )
        }
        is VersionsOperation.InvalidDelete -> {
            updateVersionsOperation(
                VersionsOperation.Delete(
                    versionsOperation.version,
                    stringResource(R.string.versions_manage_delete_version_tip_invalid)
                )
            )
        }
        is VersionsOperation.Delete -> {
            val version = versionsOperation.version
            DeleteVersionDialog(
                version = version,
                message = versionsOperation.text,
                onDismissRequest = { updateVersionsOperation(VersionsOperation.None) },
                onConfirm = { title, task ->
                    updateVersionsOperation(
                        VersionsOperation.RunTask(
                            title = title,
                            task = task
                        )
                    )
                }
            )
        }
        is VersionsOperation.RunTask -> {
            val errorMessage = stringResource(R.string.versions_manage_task_error)
            SimpleTaskDialog(
                title = stringResource(versionsOperation.title),
                task = versionsOperation.task,
                context = Dispatchers.IO,
                onDismiss = { updateVersionsOperation(VersionsOperation.None) },
                onError = { e ->
                    Log.e("VersionsOperation.RunTask", "Failed to run task. ${StringUtils.throwableToString(e)}")
                    ObjectStates.updateThrowable(
                        ObjectStates.ThrowableMessage(
                            title = errorMessage,
                            message = e.getMessageOrToString()
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun RenameVersionDialog(
    version: Version,
    onDismissRequest: () -> Unit = {},
    onConfirm: (value: String) -> Unit = {}
) {
    var name by remember { mutableStateOf(version.getVersionName()) }
    var errorMessage by remember { mutableStateOf("") }

    val isError = name.isEmpty() || isFilenameInvalid(name) { message ->
        errorMessage = message
    } || VersionsManager.validateVersionName(name, version.getVersionInfo()) { message ->
        errorMessage = message
    }

    SimpleEditDialog(
        title = stringResource(R.string.versions_manage_rename_version),
        value = name,
        onValueChange = { name = it },
        isError = isError,
        supportingText = {
            when {
                name.isEmpty() -> Text(text = stringResource(R.string.generic_cannot_empty))
                isError -> Text(text = errorMessage)
            }
        },
        singleLine = true,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            if (!isError) {
                onConfirm(name)
            }
        }
    )
}

@Composable
fun CopyVersionDialog(
    version: Version,
    onDismissRequest: () -> Unit = {},
    onConfirm: (value: String, copyAll: Boolean) -> Unit = { _, _ -> }
) {
    var copyAll by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf("") }

    val isError = name.isEmpty() || isFilenameInvalid(name) { message ->
        errorMessage = message
    } || VersionsManager.validateVersionName(name, version.getVersionInfo()) { message ->
        errorMessage = message
    }

    SimpleCheckEditDialog(
        title = stringResource(R.string.versions_manage_copy_version),
        text = stringResource(R.string.versions_manage_copy_version_tip),
        checkBoxText = stringResource(R.string.versions_manage_copy_version_all),
        checked = copyAll,
        value = name,
        onCheckedChange = { copyAll = it },
        onValueChange = { name = it },
        isError = isError,
        supportingText = {
            when {
                name.isEmpty() -> Text(text = stringResource(R.string.generic_cannot_empty))
                isError -> Text(text = errorMessage)
            }
        },
        singleLine = true,
        onDismissRequest = onDismissRequest,
        onConfirm = {
            if (!isError) {
                onConfirm(name, copyAll)
            }
        }
    )
}

@Composable
fun DeleteVersionDialog(
    version: Version,
    message: String? = null,
    onDismissRequest: () -> Unit = {},
    onConfirm: (title: Int, task: suspend () -> Unit) -> Unit = { _, _ -> },
    onVersionDeleted: () -> Unit = {}
) {
    val deleteVersion = {
        onConfirm(R.string.versions_manage_delete_version) {
            VersionsManager.deleteVersion(version)
            onVersionDeleted()
        }
    }

    if (message != null) {
        SimpleAlertDialog(
            title = stringResource(R.string.versions_manage_delete_version),
            text = message,
            onDismiss = onDismissRequest,
            onConfirm = deleteVersion
        )
    } else {
        SimpleAlertDialog(
            title = stringResource(R.string.versions_manage_delete_version),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(R.string.versions_manage_delete_version_tip_hint1, version.getVersionName()))
                    Text(text = stringResource(R.string.versions_manage_delete_version_tip_hint2))
                    Text(text = stringResource(R.string.versions_manage_delete_version_tip_hint3))
                    Text(
                        text = stringResource(R.string.versions_manage_delete_version_tip_hint4),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            onConfirm = deleteVersion,
            onCancel = onDismissRequest,
            onDismissRequest = onDismissRequest
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VersionItemLayout(
    version: Version,
    selected: Boolean,
    modifier: Modifier = Modifier,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onSelected: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }
    Surface(
        modifier = modifier.graphicsLayer(scaleY = scale.value, scaleX = scale.value),
        color = color,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large,
        shadowElevation = 1.dp,
        onClick = {
            if (selected) return@Surface
            onSelected()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = MaterialTheme.shapes.large)
                .padding(all = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = {
                    if (selected) return@RadioButton
                    onSelected()
                }
            )
            VersionIconImage(
                version = version,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                //版本名称
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    overflow = TextOverflow.Clip,
                    maxLines = 1,
                    text = version.getVersionName(),
                    style = MaterialTheme.typography.labelLarge
                )
                //版本描述
                if (version.isValid() && version.isSummaryValid()) {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        overflow = TextOverflow.Clip,
                        maxLines = 1,
                        text = version.getVersionSummary(),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                //版本详细信息
                FlowRow {
                    if (!version.isValid()) {
                        Text(
                            text = stringResource(R.string.versions_manage_invalid),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    if (version.getVersionConfig().isIsolation()) {
                        Text(
                            text = stringResource(R.string.versions_manage_isolation_enabled),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    version.getVersionInfo()?.let { versionInfo ->
                        Text(
                            text = versionInfo.minecraftVersion,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        versionInfo.loaderInfo?.let { loaderInfo ->
                            Text(
                                text = loaderInfo.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = loaderInfo.version,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                }
            }
            if (version.isValid()) {
                IconButton(
                    onClick = onSettingsClick
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.versions_manage_settings)
                    )
                }
            }
            Row {
                var menuExpanded by remember { mutableStateOf(false) }

                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                    Icon(
                        modifier = Modifier.size(24.dp),
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
                        text = { Text(text = stringResource(R.string.generic_copy)) },
                        leadingIcon = {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Filled.FileCopy,
                                contentDescription = stringResource(R.string.generic_copy)
                            )
                        },
                        onClick = {
                            onCopyClick()
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
    }
}

@Composable
fun VersionIconImage(
    version: Version?,
    modifier: Modifier = Modifier,
    refreshKey: Any? = null
) {
    val context = LocalContext.current

    val imageLoader = remember(refreshKey, context) {
        ImageLoader.Builder(context)
            .components { add(GifDecoder.Factory()) }
            .build()
    }

    val (model, fallbackRes) = remember(refreshKey, context) {
        when {
            version == null -> null to R.drawable.ic_minecraft
            else -> {
                val iconFile = VersionsManager.getVersionIconFile(version)
                if (iconFile.exists()) {
                    val imageModel = ImageRequest.Builder(context)
                        .data(iconFile)
                        .build()
                    imageModel to null
                } else {
                    null to getLoaderIconRes(version)
                }
            }
        }
    }

    if (model != null) {
        AsyncImage(
            model = model,
            imageLoader = imageLoader,
            modifier = modifier,
            contentScale = ContentScale.Inside,
            contentDescription = null
        )
    } else {
        Image(
            painter = painterResource(id = fallbackRes ?: R.drawable.ic_minecraft),
            modifier = modifier,
            contentScale = ContentScale.Inside,
            contentDescription = null
        )
    }
}

private fun getLoaderIconRes(version: Version): Int {
    val loaderName = version.getVersionInfo()?.loaderInfo?.name?.lowercase() ?: ""
    return when(loaderName) {
        "fabric" -> R.drawable.ic_fabric
        "forge" -> R.drawable.ic_anvil
        "quilt" -> R.drawable.ic_quilt
        "neoforge" -> R.drawable.ic_neoforge
        "optifine" -> R.drawable.ic_optifine
        "liteloader" -> R.drawable.ic_chicken_old
        else -> R.drawable.ic_minecraft
    }
}
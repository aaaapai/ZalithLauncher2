package com.movtery.zalithlauncher.ui.screens.content.versions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.copyLocalFile
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.game.version.installed.VersionFolders
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SimpleEditDialog
import com.movtery.zalithlauncher.ui.components.SimpleTaskDialog
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.elements.DeleteVersionDialog
import com.movtery.zalithlauncher.ui.screens.content.elements.RenameVersionDialog
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.VersionSettingsBackground
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.file.ensureDirectory
import com.movtery.zalithlauncher.utils.file.shareFile
import com.movtery.zalithlauncher.utils.logging.Logger.lError
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import kotlinx.coroutines.Dispatchers
import org.apache.commons.io.FileUtils
import java.io.File

@Composable
fun VersionOverViewScreen(
    mainScreenKey: NavKey?,
    versionsScreenKey: NavKey?,
    backToMainScreen: () -> Unit,
    version: Version,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.Versions::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.Versions.OverView, versionsScreenKey, false)
    ) { isVisible ->
        var versionSummary by remember { mutableStateOf(version.getVersionSummary()) }
        var refreshVersionIcon by remember { mutableIntStateOf(0) }

        val context = LocalContext.current
        var iconFileExists by remember { mutableStateOf(VersionsManager.getVersionIconFile(version).exists()) }

        var versionsOperation by remember { mutableStateOf<VersionsOperation>(VersionsOperation.None) }
        VersionsOperation(
            versionsOperation = versionsOperation,
            updateOperation = { versionsOperation = it },
            summitError = summitError,
            onIconPicked = {
                refreshVersionIcon++
                iconFileExists = VersionsManager.getVersionIconFile(version).exists()
                versionsOperation = VersionsOperation.None
            },
            resetIcon = {
                val iconFile = VersionsManager.getVersionIconFile(version)
                FileUtils.deleteQuietly(iconFile)
                refreshVersionIcon++
                iconFileExists = iconFile.exists()
            },
            onVersionRefreshed = backToMainScreen,
            setVersionSummary = { value ->
                version.getVersionConfig().apply {
                    this.versionSummary = value
                    save()
                }
                versionSummary = version.getVersionSummary()
            },
            onVersionDeleted = backToMainScreen
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
                .padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val yOffset1 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible
            )

            VersionInfoLayout(
                modifier = Modifier.offset { IntOffset(x = 0, y = yOffset1.roundToPx()) },
                version, versionSummary, iconFileExists, refreshVersionIcon,
                pickIcon = { versionsOperation = VersionsOperation.PickIcon(version) },
                resetIcon = { versionsOperation = VersionsOperation.ResetIconAlert }
            )

            val yOffset2 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible,
                delayMillis = 50
            )

            VersionManagementLayout(
                modifier = Modifier.offset { IntOffset(x = 0, y = yOffset2.roundToPx()) },
                onEditSummary = { versionsOperation = VersionsOperation.EditSummary(version) },
                onRename = { versionsOperation = VersionsOperation.Rename(version) },
                onDelete = { versionsOperation = VersionsOperation.Delete(version) }
            )

            val yOffset3 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible,
                delayMillis = 100
            )

            VersionQuickActions(
                modifier = Modifier.offset { IntOffset(x = 0, y = yOffset3.roundToPx()) },
                accessFolder = { path ->
                    val folder = File(version.getGameDir(), path)
                    runCatching {
                        folder.ensureDirectory()
                    }.onFailure { e ->
                        summitError(
                            ErrorViewModel.ThrowableMessage(
                                title = context.getString(R.string.error_create_dir, folder.absolutePath),
                                message = e.getMessageOrToString()
                            )
                        )
                        return@VersionQuickActions
                    }
                    shareFile(context, folder)
                }
            )
        }
    }
}

@Composable
private fun PickIcon(
    version: Version,
    onDone: () -> Unit,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current

    val iconFile = VersionsManager.getVersionIconFile(version)
    val iconPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { result ->
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = {
                        context.copyLocalFile(result, iconFile)
                    },
                    onError = { e ->
                        lError("Failed to import icon!", e)
                        FileUtils.deleteQuietly(iconFile)
                        summitError(
                            ErrorViewModel.ThrowableMessage(
                                title = context.getString(R.string.error_import_image),
                                message = e.getMessageOrToString()
                            )
                        )
                    },
                    onFinally = onDone
                )
            )
        }
    }

    LaunchedEffect(iconPicker) {
        iconPicker.launch(arrayOf("image/*"))
    }
}

@Composable
private fun VersionInfoLayout(
    modifier: Modifier = Modifier,
    version: Version,
    versionSummary: String,
    iconFileExists: Boolean,
    refreshKey: Any? = null,
    pickIcon: () -> Unit = {},
    resetIcon: () -> Unit = {}
) {
    VersionSettingsBackground(
        modifier = modifier,
        paddingValues = PaddingValues(all = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VersionOverviewItem(
                modifier = Modifier.padding(start = 4.dp).weight(1f),
                version = version,
                versionSummary = versionSummary,
                refreshKey = refreshKey
            )
            Row {
                IconTextButton(
                    onClick = pickIcon,
                    imageVector = Icons.Outlined.Image,
                    contentDescription = stringResource(R.string.versions_overview_custom_version_icon),
                    text = stringResource(R.string.versions_overview_custom_version_icon)
                )
                if (iconFileExists) {
                    Spacer(modifier = Modifier.width(12.dp))
                    IconTextButton(
                        onClick = resetIcon,
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = stringResource(R.string.versions_overview_reset_version_icon),
                        text = stringResource(R.string.versions_overview_reset_version_icon)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VersionManagementLayout(
    modifier: Modifier = Modifier,
    onEditSummary: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    VersionSettingsBackground(
        modifier = modifier,
        paddingValues = PaddingValues(all = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp).padding(top = 4.dp, bottom = 8.dp),
                text = stringResource(R.string.versions_settings_overview_management),
                style = MaterialTheme.typography.labelLarge
            )

            FlowRow {
                OutlinedButton(
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = onEditSummary
                ) {
                    Text(
                        text = stringResource(R.string.versions_overview_edit_version_summary)
                    )
                }
                OutlinedButton(
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = onRename
                ) {
                    Text(
                        text = stringResource(R.string.versions_manage_rename_version)
                    )
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.versions_manage_delete_version)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VersionQuickActions(
    modifier: Modifier = Modifier,
    accessFolder: (folderName: String) -> Unit = {}
) {
    VersionSettingsBackground(
        modifier = modifier,
        paddingValues = PaddingValues(all = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 8.dp).padding(top = 4.dp, bottom = 8.dp),
                text = stringResource(R.string.versions_settings_overview_quick_actions),
                style = MaterialTheme.typography.labelLarge
            )

            FlowRow {
                OutlinedButton(
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = { accessFolder("") }
                ) {
                    Text(
                        text = stringResource(R.string.versions_overview_version_folder)
                    )
                }
                OutlinedButton(
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = { accessFolder(VersionFolders.SAVES.folderName) }
                ) {
                    Text(
                        text = stringResource(R.string.versions_overview_saves_folder)
                    )
                }
                OutlinedButton(
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = { accessFolder(VersionFolders.RESOURCE_PACK.folderName) }
                ) {
                    Text(
                        text = stringResource(R.string.versions_overview_resource_pack_folder)
                    )
                }
                OutlinedButton(
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = { accessFolder(VersionFolders.SHADERS.folderName) }
                ) {
                    Text(
                        text = stringResource(R.string.versions_overview_shaders_pack_folder)
                    )
                }
                OutlinedButton(
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = { accessFolder("screenshots") }
                ) {
                    Text(
                        text = stringResource(R.string.versions_overview_screenshot_folder)
                    )
                }
                OutlinedButton(
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = { accessFolder("logs") }
                ) {
                    Text(
                        text = stringResource(R.string.versions_overview_logs_folder)
                    )
                }
                OutlinedButton(
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = { accessFolder("crash-reports") }
                ) {
                    Text(
                        text = stringResource(R.string.versions_overview_crash_report_folder)
                    )
                }
            }
        }
    }
}

/**
 * 版本概览操作
 */
sealed interface VersionsOperation {
    data object None: VersionsOperation
    data class PickIcon(val version: Version): VersionsOperation
    data object ResetIconAlert: VersionsOperation
    data object ResetIcon: VersionsOperation
    data class EditSummary(val version: Version): VersionsOperation
    data class Rename(val version: Version): VersionsOperation
    data class Delete(val version: Version): VersionsOperation
    data class RunTask(val title: Int, val task: suspend () -> Unit): VersionsOperation
}

@Composable
private fun VersionsOperation(
    versionsOperation: VersionsOperation,
    updateOperation: (VersionsOperation) -> Unit,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit,
    onIconPicked: () -> Unit = {},
    resetIcon: () -> Unit = {},
    onVersionRefreshed: () -> Unit = {},
    setVersionSummary: (String) -> Unit = {},
    onVersionDeleted: () -> Unit = {}
) {
    when(versionsOperation) {
        is VersionsOperation.None -> {}
        is VersionsOperation.PickIcon -> {
            PickIcon(
                version = versionsOperation.version,
                onDone = onIconPicked,
                summitError = summitError
            )
        }
        is VersionsOperation.ResetIconAlert -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_reset),
                text = stringResource(R.string.versions_overview_reset_version_icon_message),
                onDismiss = { updateOperation(VersionsOperation.None) },
                onConfirm = { updateOperation(VersionsOperation.ResetIcon) }
            )
        }
        is VersionsOperation.ResetIcon -> resetIcon()
        is VersionsOperation.Rename -> {
            RenameVersionDialog(
                version = versionsOperation.version,
                onDismissRequest = { updateOperation(VersionsOperation.None) },
                onConfirm = {
                    updateOperation(
                        VersionsOperation.RunTask(
                            title = R.string.versions_manage_rename_version,
                            task = {
                                VersionsManager.renameVersion(versionsOperation.version, it)
                                onVersionRefreshed()
                            }
                        )
                    )
                }
            )
        }
        is VersionsOperation.Delete -> {
            val version = versionsOperation.version

            DeleteVersionDialog(
                version = version,
                onDismissRequest = { updateOperation(VersionsOperation.None) },
                onConfirm = { title, task ->
                    updateOperation(
                        VersionsOperation.RunTask(
                            title = title,
                            task = task
                        )
                    )
                },
                onVersionDeleted = onVersionDeleted
            )
        }
        is VersionsOperation.EditSummary -> {
            val version = versionsOperation.version
            var value by remember { mutableStateOf(version.getVersionConfig().versionSummary) }

            SimpleEditDialog(
                title = stringResource(R.string.versions_overview_edit_version_summary),
                value = value,
                onValueChange = { value = it },
                label = {
                    Text(text = stringResource(R.string.versions_overview_edit_version_summary_label))
                },
                singleLine = true,
                onDismissRequest = { updateOperation(VersionsOperation.None) },
                onConfirm = {
                    setVersionSummary(value)
                    updateOperation(VersionsOperation.None)
                }
            )
        }
        is VersionsOperation.RunTask -> {
            val errorMessage = stringResource(R.string.versions_manage_task_error)
            SimpleTaskDialog(
                title = stringResource(versionsOperation.title),
                task = versionsOperation.task,
                context = Dispatchers.IO,
                onDismiss = { updateOperation(VersionsOperation.None) },
                onError = { e ->
                    lError("Failed to run task.", e)
                    summitError(
                        ErrorViewModel.ThrowableMessage(
                            title = errorMessage,
                            message = e.getMessageOrToString()
                        )
                    )
                }
            )
        }
    }
}
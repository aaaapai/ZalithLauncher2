package com.movtery.zalithlauncher.ui.screens.content.settings

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.ZLApplication
import com.movtery.zalithlauncher.context.getFileName
import com.movtery.zalithlauncher.contract.ExtensionFilteredDocumentPicker
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.game.launch.JvmLauncher
import com.movtery.zalithlauncher.game.multirt.Runtime
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.state.ObjectStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.itemLayoutColor
import com.movtery.zalithlauncher.ui.screens.content.SETTINGS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.string.StringUtils
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.getMessageOrToString
import kotlinx.coroutines.Dispatchers

const val JAVA_MANAGE_SCREEN_TAG = "JavaManageScreen"

sealed interface RuntimeOperation {
    data object None: RuntimeOperation
    data class PreDelete(val runtime: Runtime): RuntimeOperation
    data class Delete(val runtime: Runtime): RuntimeOperation
    data class ProgressUri(val uris: List<Uri>): RuntimeOperation
    data class ExecuteJar(val uri: Uri): RuntimeOperation
}

@Composable
fun JavaManageScreen() {
    BaseScreen(
        parentScreenTag = SETTINGS_SCREEN_TAG,
        parentCurrentTag = MutableStates.mainScreenTag,
        childScreenTag = JAVA_MANAGE_SCREEN_TAG,
        childCurrentTag = MutableStates.settingsScreenTag
    ) { isVisible ->
        val yOffset by swapAnimateDpAsState(
            targetValue = (-40).dp,
            swapIn = isVisible
        )

        var runtimes by remember { mutableStateOf(getRuntimes()) }
        var runtimeOperation by remember { mutableStateOf<RuntimeOperation>(RuntimeOperation.None) }
        RuntimeOperation(
            runtimeOperation = runtimeOperation,
            updateOperation = { runtimeOperation = it },
            callRefresh = { runtimes = getRuntimes(true) }
        )

        val runtimePicker = rememberLauncherForActivityResult(
            contract = ExtensionFilteredDocumentPicker(extension = "xz", allowMultiple = true)
        ) { uris ->
            uris.takeIf { it.isNotEmpty() }?.let {
                runtimeOperation = RuntimeOperation.ProgressUri(it)
            }
        }

        val jarPicker = rememberLauncherForActivityResult(
            contract = ExtensionFilteredDocumentPicker(extension = "jar", allowMultiple = false)
        ) { uris ->
            uris.takeIf { it.isNotEmpty() }?.get(0)?.let { uri ->
                runtimeOperation = RuntimeOperation.ExecuteJar(uri)
            }
        }

        SettingsBackground(
            modifier = Modifier
                .fillMaxHeight()
                .padding(all = 12.dp)
                .offset {
                    IntOffset(
                        x = 0,
                        y = yOffset.roundToPx()
                    )
                },
            contentPadding = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            ) {
                IconTextButton(
                    onClick = { runtimes = getRuntimes(true) },
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.generic_refresh),
                    text = stringResource(R.string.generic_refresh),
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconTextButton(
                    onClick = { runtimePicker.launch("") },
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.generic_import),
                    text = stringResource(R.string.generic_import),
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconTextButton(
                    onClick = { jarPicker.launch("") },
                    imageVector = Icons.Default.Terminal,
                    contentDescription = stringResource(R.string.execute_jar_title),
                    text = stringResource(R.string.execute_jar_title),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(all = 12.dp)
            ) {
                items(runtimes.size) { index ->
                    JavaRuntimeItem(
                        runtime = runtimes[index],
                        modifier = Modifier
                            .padding(bottom = if (index == runtimes.size - 1) 0.dp else 12.dp),
                        onDeleteClick = {
                            runtimeOperation = RuntimeOperation.PreDelete(runtimes[index])
                        }
                    )
                }
            }
        }
    }
}

private fun getRuntimes(forceLoad: Boolean = false): List<Runtime> =
    RuntimesManager.getRuntimes(forceLoad = forceLoad)

@Composable
private fun RuntimeOperation(
    runtimeOperation: RuntimeOperation,
    updateOperation: (RuntimeOperation) -> Unit,
    callRefresh: () -> Unit
) {
    when(runtimeOperation) {
        is RuntimeOperation.None -> {}
        is RuntimeOperation.PreDelete -> {
            val runtime = runtimeOperation.runtime
            SimpleAlertDialog(
                title = stringResource(R.string.generic_warning),
                text = stringResource(R.string.multirt_runtime_delete_message, runtime.name),
                onConfirm = { updateOperation(RuntimeOperation.Delete(runtime)) },
                onDismiss = { updateOperation(RuntimeOperation.None) }
            )
        }
        is RuntimeOperation.Delete -> {
            val failedMessage = stringResource(R.string.multirt_runtime_delete_failed)
            val runtime = runtimeOperation.runtime
            TaskSystem.submitTask(
                Task.runTask(
                    id = runtime.name,
                    dispatcher = Dispatchers.IO,
                    task = { task ->
                        task.updateMessage(R.string.multirt_runtime_deleting, runtime.name)
                        RuntimesManager.removeRuntime(runtime.name)
                    },
                    onError = {
                        ObjectStates.updateThrowable(
                            ObjectStates.ThrowableMessage(
                                title = failedMessage,
                                message = it.getMessageOrToString()
                            )
                        )
                    },
                    onFinally = callRefresh
                )
            )
            updateOperation(RuntimeOperation.None)
        }
        is RuntimeOperation.ProgressUri -> {
            val context = LocalContext.current
            runtimeOperation.uris.forEach { uri ->
                progressRuntimeUri(
                    context = context,
                    uri = uri,
                    callRefresh = callRefresh
                )
            }
            updateOperation(RuntimeOperation.None)
        }
        is RuntimeOperation.ExecuteJar -> {
            val context = LocalContext.current
            RuntimesManager.getExactJreName(8) ?: run {
                Toast.makeText(context, R.string.multirt_no_java_8, Toast.LENGTH_LONG).show()
                updateOperation(RuntimeOperation.None)
                return
            }
            (context as? Activity)?.let { activity ->
                val jreName = AllSettings.javaRuntime.takeIf { AllSettings.autoPickJavaRuntime.getValue() }?.getValue()
                JvmLauncher.executeJarWithUri(activity, runtimeOperation.uri, jreName)
            }
            updateOperation(RuntimeOperation.None)
        }
    }
}

private fun progressRuntimeUri(
    context: Context,
    uri: Uri,
    callRefresh: () -> Unit
) {
    fun showError(message: String) {
        ObjectStates.updateThrowable(
            ObjectStates.ThrowableMessage(
                title = context.getString(R.string.multirt_runtime_import_failed),
                message = message
            )
        )
    }

    val name = context.getFileName(uri) ?: run {
        showError(context.getString(R.string.multirt_runtime_import_failed_file_name))
        return
    }
    TaskSystem.submitTask(
        Task.runTask(
            id = name,
            dispatcher = Dispatchers.IO,
            task = { task ->
                val inputStream = context.contentResolver.openInputStream(uri) ?: run {
                    showError(context.getString(R.string.multirt_runtime_import_failed_input_stream))
                    return@runTask
                }
                RuntimesManager.installRuntime(
                    nativeLibDir = PathManager.DIR_NATIVE_LIB,
                    inputStream = inputStream,
                    name = name,
                    updateProgress = { textRes, textArg ->
                        task.updateMessage(textRes, *textArg)
                    }
                )
            },
            onError = {
                showError(StringUtils.throwableToString(it))
            },
            onFinally = callRefresh,
            onCancel = {
                runCatching {
                    RuntimesManager.removeRuntime(name)
                    callRefresh()
                }.onFailure { t ->
                    ObjectStates.updateThrowable(
                        ObjectStates.ThrowableMessage(
                            title = context.getString(R.string.multirt_runtime_delete_failed),
                            message = t.getMessageOrToString()
                        )
                    )
                }
            }
        )
    )
}

@Composable
private fun JavaRuntimeItem(
    runtime: Runtime,
    modifier: Modifier = Modifier,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {},
    onDeleteClick: () -> Unit
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
        onClick = onClick
    ) {
        Row {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp))
            ) {
                Text(
                    text = runtime.name,
                    style = MaterialTheme.typography.labelMedium
                )
                //环境标签
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (runtime.isProvidedByLauncher) {
                        Text(
                            text = stringResource(R.string.multirt_runtime_provided_by_launcher),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        text =  runtime.versionString?.let {
                            stringResource(R.string.multirt_runtime_version_name, it)
                        } ?: stringResource(R.string.multirt_runtime_corrupt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (runtime.versionString != null) contentColor else MaterialTheme.colorScheme.error
                    )
                    runtime.javaVersion.takeIf { it != 0 }?.let { javaVersion ->
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.multirt_runtime_version_code, javaVersion),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    runtime.arch?.let { arch ->
                        val compatible = ZLApplication.DEVICE_ARCHITECTURE == Architecture.archAsInt(arch)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = arch.takeIf {
                                compatible
                            }?.let {
                                stringResource(R.string.multirt_runtime_version_arch, it)
                            } ?: stringResource(R.string.multirt_runtime_incompatible_arch, arch),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (compatible) contentColor else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            IconButton(
                //内置环境（未损坏）无法删除
                enabled = !runtime.isProvidedByLauncher || !runtime.isCompatible,
                onClick = onDeleteClick
            ) {
                Icon(
                    modifier = Modifier.padding(all = 8.dp),
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.generic_delete)
                )
            }
        }
    }
}
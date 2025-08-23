package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.copyLocalFile
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.GestureActionType
import com.movtery.zalithlauncher.setting.enums.MouseControlMode
import com.movtery.zalithlauncher.ui.activities.MainActivity
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.LittleTextLabel
import com.movtery.zalithlauncher.ui.components.MarqueeText
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.ui.components.TooltipIconButton
import com.movtery.zalithlauncher.ui.components.infiniteShimmer
import com.movtery.zalithlauncher.ui.control.mouse.MousePointer
import com.movtery.zalithlauncher.ui.control.mouse.mousePointerFile
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.formatKeyCode
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.getMessageOrToString
import com.movtery.zalithlauncher.viewmodel.ErrorViewModel
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterIsInstance
import org.apache.commons.io.FileUtils

@Composable
fun ControlSettingsScreen(
    key: NestedNavKey.Settings,
    settingsScreenKey: NavKey?,
    mainScreenKey: NavKey?,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    BaseScreen(
        Triple(key, mainScreenKey, false),
        Triple(NormalNavKey.Settings.Control, settingsScreenKey, false)
    ) { isVisible ->
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

            SettingsBackground(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = yOffset1.roundToPx()
                        )
                    }
            ) {
                SwitchSettingsLayout(
                    unit = AllSettings.physicalMouseMode,
                    title = stringResource(R.string.settings_control_mouse_physical_mouse_mode_title),
                    summary = stringResource(R.string.settings_control_mouse_physical_mouse_mode_summary),
                    trailingIcon = {
                        TooltipIconButton(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            tooltipTitle = stringResource(R.string.generic_warning),
                            tooltipMessage = stringResource(R.string.settings_control_mouse_physical_mouse_warning)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = stringResource(R.string.generic_warning),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                )

                var operation by remember { mutableStateOf<PhysicalKeyOperation>(PhysicalKeyOperation.None) }
                PhysicalKeyImeTrigger(
                    modifier = Modifier.fillMaxWidth(),
                    operation = operation,
                    changeOperation = { operation = it }
                )
            }

            val yOffset2 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible,
                delayMillis = 50
            )

            SettingsBackground(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = yOffset2.roundToPx()
                        )
                    }
            ) {
                SwitchSettingsLayout(
                    unit = AllSettings.hideMouse,
                    title = stringResource(R.string.settings_control_mouse_hide_title),
                    summary = stringResource(R.string.settings_control_mouse_hide_summary),
                    enabled = AllSettings.mouseControlMode.state == MouseControlMode.CLICK //仅点击模式下可更改设置
                )

                var mouseOperation by remember { mutableStateOf<MousePointerOperation>(MousePointerOperation.None) }
                MousePointerLayout(
                    mouseSize = AllSettings.mouseSize.state,
                    mouseOperation = mouseOperation,
                    changeOperation = { mouseOperation = it },
                    summitError = summitError
                )

                SliderSettingsLayout(
                    unit = AllSettings.mouseSize,
                    title = stringResource(R.string.settings_control_mouse_size_title),
                    valueRange = 5f..50f,
                    suffix = "Dp",
                    fineTuningControl = true
                )

                ListSettingsLayout(
                    unit = AllSettings.mouseControlMode,
                    items = MouseControlMode.entries,
                    title = stringResource(R.string.settings_control_mouse_control_mode_title),
                    summary = stringResource(R.string.settings_control_mouse_control_mode_summary),
                    getItemText = { stringResource(it.nameRes) }
                )

                SliderSettingsLayout(
                    unit = AllSettings.cursorSensitivity,
                    title = stringResource(R.string.settings_control_mouse_sensitivity_title),
                    summary = stringResource(R.string.settings_control_mouse_sensitivity_summary),
                    valueRange = 25f..300f,
                    suffix = "%",
                    fineTuningControl = true
                )

                SliderSettingsLayout(
                    unit = AllSettings.mouseCaptureSensitivity,
                    title = stringResource(R.string.settings_control_mouse_capture_sensitivity_title),
                    summary = stringResource(R.string.settings_control_mouse_capture_sensitivity_summary),
                    valueRange = 25f..300f,
                    suffix = "%",
                    fineTuningControl = true
                )

                SliderSettingsLayout(
                    unit = AllSettings.mouseLongPressDelay,
                    title = stringResource(R.string.settings_control_mouse_long_press_delay_title),
                    summary = stringResource(R.string.settings_control_mouse_long_press_delay_summary),
                    valueRange = 100f..1000f,
                    suffix = "ms",
                    fineTuningControl = true
                )
            }

            val yOffset3 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible,
                delayMillis = 100
            )

            SettingsBackground(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = yOffset3.roundToPx()
                        )
                    }
            ) {
                SwitchSettingsLayout(
                    unit = AllSettings.gestureControl,
                    title = stringResource(R.string.settings_control_gesture_control_title),
                    summary = stringResource(R.string.settings_control_gesture_control_summary)
                )

                ListSettingsLayout(
                    unit = AllSettings.gestureTapMouseAction,
                    items = GestureActionType.entries,
                    title = stringResource(R.string.settings_control_gesture_tap_action_title),
                    summary = stringResource(R.string.settings_control_gesture_tap_action_summary),
                    getItemText = { stringResource(it.nameRes) },
                    enabled = AllSettings.gestureControl.state
                )

                ListSettingsLayout(
                    unit = AllSettings.gestureLongPressMouseAction,
                    items = GestureActionType.entries,
                    title = stringResource(R.string.settings_control_gesture_long_press_action_title),
                    summary = stringResource(R.string.settings_control_gesture_long_press_action_summary),
                    getItemText = { stringResource(it.nameRes) },
                    enabled = AllSettings.gestureControl.state
                )

                SliderSettingsLayout(
                    unit = AllSettings.gestureLongPressDelay,
                    title = stringResource(R.string.settings_control_gesture_long_press_delay_title),
                    summary = stringResource(R.string.settings_control_mouse_long_press_delay_summary),
                    valueRange = 100f..1000f,
                    suffix = "ms",
                    enabled = AllSettings.gestureControl.state,
                    fineTuningControl = true
                )
            }
        }
    }
}

private sealed interface PhysicalKeyOperation {
    data object None: PhysicalKeyOperation
    data object Bind: PhysicalKeyOperation
}

@Composable
private fun PhysicalKeyImeTrigger(
    modifier: Modifier = Modifier,
    operation: PhysicalKeyOperation,
    changeOperation: (PhysicalKeyOperation) -> Unit
) {
    Row(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(shape = RoundedCornerShape(22.0.dp))
                .clickable { changeOperation(PhysicalKeyOperation.Bind) }
                .padding(all = 8.dp)
                .padding(bottom = 4.dp)
                .animateContentSize()
        ) Column@{
            TitleAndSummary(
                title = stringResource(R.string.settings_control_physical_key_bind_ime_title),
                summary = stringResource(R.string.settings_control_physical_key_bind_ime_summary)
            )
            when (operation) {
                PhysicalKeyOperation.None -> {}
                PhysicalKeyOperation.Bind -> {
                    val activity = LocalActivity.current as? MainActivity ?: run {
                        changeOperation(PhysicalKeyOperation.None)
                        return@Column //无法通过Activity获取更精准的按键键值
                    }
                    val eventViewModel = activity.eventViewModel

                    LaunchedEffect(Unit) {
                        eventViewModel.sendEvent(EventViewModel.Event.Key.StartKeyCapture)
                        //接收Activity发送的按键事件
                        eventViewModel.events
                            .filterIsInstance<EventViewModel.Event.Key.OnKeyDown>()
                            .collect { event ->
                                changeOperation(PhysicalKeyOperation.None)
                                AllSettings.physicalKeyImeCode.save(event.key.keyCode)
                            }
                    }

                    DisposableEffect(Unit) {
                        onDispose {
                            eventViewModel.sendEvent(EventViewModel.Event.Key.StopKeyCapture)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LittleTextLabel(text = stringResource(R.string.control_keyboard_bind_title))
                        MarqueeText(
                            modifier = Modifier
                                .weight(1f)
                                .infiniteShimmer(
                                    initialValue = 0.5f,
                                    targetValue = 1f
                                ),
                            text = stringResource(R.string.control_keyboard_bind_summary),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(start = 8.dp, end = 4.dp)
                .align(Alignment.CenterVertically)
        ) {
            val code = AllSettings.physicalKeyImeCode.state
            when {
                code == null -> {
                    Text(
                        modifier = Modifier.padding(end = 12.dp),
                        text = stringResource(R.string.settings_control_physical_key_bind_ime_un_bind),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                else -> {
                    IconTextButton(
                        onClick = { AllSettings.physicalKeyImeCode.save(null) },
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = stringResource(R.string.generic_reset),
                        text = stringResource(
                            R.string.settings_control_physical_key_bind_ime_bound,
                            formatKeyCode(code)
                        )
                    )
                }
            }
        }
    }
}

private sealed interface MousePointerOperation {
    data object None: MousePointerOperation
    data object Reset: MousePointerOperation
    data object Refresh: MousePointerOperation
}

@Composable
private fun MousePointerLayout(
    mouseSize: Int,
    mouseOperation: MousePointerOperation,
    changeOperation: (MousePointerOperation) -> Unit,
    summitError: (ErrorViewModel.ThrowableMessage) -> Unit
) {
    val context = LocalContext.current

    var triggerState by remember { mutableIntStateOf(0) }

    when (mouseOperation) {
        is MousePointerOperation.None -> {}
        is MousePointerOperation.Reset -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_reset),
                text = stringResource(R.string.settings_control_mouse_pointer_reset_message),
                onConfirm = {
                    FileUtils.deleteQuietly(mousePointerFile)
                    changeOperation(MousePointerOperation.Refresh)
                },
                onDismiss = { changeOperation(MousePointerOperation.None) }
            )
        }
        is MousePointerOperation.Refresh -> {
            triggerState++
            changeOperation(MousePointerOperation.None)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { result ->
        if (result != null) {
            TaskSystem.submitTask(
                Task.runTask(
                    dispatcher = Dispatchers.IO,
                    task = {
                        context.copyLocalFile(result, mousePointerFile)
                        changeOperation(MousePointerOperation.Refresh)
                    },
                    onError = { th ->
                        FileUtils.deleteQuietly(mousePointerFile)
                        summitError(
                            ErrorViewModel.ThrowableMessage(
                                title = context.getString(R.string.error_import_image),
                                message = th.getMessageOrToString()
                            )
                        )
                    }
                )
            )
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(shape = RoundedCornerShape(22.0.dp))
                .clickable { filePicker.launch(arrayOf("image/*")) }
                .padding(all = 8.dp)
                .padding(bottom = 4.dp)
        ) {
            TitleAndSummary(
                title = stringResource(R.string.settings_control_mouse_pointer_title),
                summary = stringResource(R.string.settings_control_mouse_pointer_summary)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MousePointer(
                modifier = Modifier.padding(all = 8.dp),
                mouseSize = mouseSize.dp,
                mouseFile = mousePointerFile,
                centerIcon = true,
                triggerRefresh = triggerState
            )

            val mouseExists = remember(triggerState) { mousePointerFile.exists() }

            if (mouseExists) {
                IconTextButton(
                    onClick = {
                        changeOperation(MousePointerOperation.Reset)
                    },
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = stringResource(R.string.generic_reset),
                    text = stringResource(R.string.generic_reset)
                )
            }
        }
    }
}
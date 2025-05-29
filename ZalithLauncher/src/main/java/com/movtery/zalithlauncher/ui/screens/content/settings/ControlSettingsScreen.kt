package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
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
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.context.copyLocalFile
import com.movtery.zalithlauncher.coroutine.Task
import com.movtery.zalithlauncher.coroutine.TaskSystem
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.GestureActionType
import com.movtery.zalithlauncher.setting.enums.MouseControlMode
import com.movtery.zalithlauncher.setting.gestureControl
import com.movtery.zalithlauncher.setting.gestureLongPressDelay
import com.movtery.zalithlauncher.setting.gestureLongPressMouseAction
import com.movtery.zalithlauncher.setting.gestureTapMouseAction
import com.movtery.zalithlauncher.setting.mouseControlMode
import com.movtery.zalithlauncher.setting.mouseLongPressDelay
import com.movtery.zalithlauncher.setting.mouseSize
import com.movtery.zalithlauncher.setting.mouseSpeed
import com.movtery.zalithlauncher.setting.physicalMouseMode
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.state.ObjectStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.IconTextButton
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.ui.components.TooltipIconButton
import com.movtery.zalithlauncher.ui.control.mouse.MousePointer
import com.movtery.zalithlauncher.ui.control.mouse.mousePointerFile
import com.movtery.zalithlauncher.ui.screens.content.SETTINGS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.getMessageOrToString
import kotlinx.coroutines.Dispatchers
import org.apache.commons.io.FileUtils

const val CONTROL_SETTINGS_SCREEN_TAG = "ControlSettingsScreen"

@Composable
fun ControlSettingsScreen() {
    BaseScreen(
        parentScreenTag = SETTINGS_SCREEN_TAG,
        parentCurrentTag = MutableStates.mainScreenTag,
        childScreenTag = CONTROL_SETTINGS_SCREEN_TAG,
        childCurrentTag = MutableStates.settingsScreenTag
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
                    },
                    onCheckedChange = {
                        physicalMouseMode = it
                    }
                )

                MousePointerLayout(
                    mouseSize = mouseSize
                )

                SliderSettingsLayout(
                    unit = AllSettings.mouseSize,
                    title = stringResource(R.string.settings_control_mouse_size_title),
                    valueRange = 5f..50f,
                    suffix = "Dp",
                    fineTuningControl = true,
                    onValueChange = { mouseSize = it }
                )

                ListSettingsLayout(
                    unit = AllSettings.mouseControlMode,
                    items = MouseControlMode.entries,
                    title = stringResource(R.string.settings_control_mouse_control_mode_title),
                    summary = stringResource(R.string.settings_control_mouse_control_mode_summary),
                    getItemId = { it.name },
                    getItemText = { stringResource(it.nameRes) },
                    onValueChange = { mouseControlMode = it }
                )

                SliderSettingsLayout(
                    unit = AllSettings.mouseSpeed,
                    title = stringResource(R.string.settings_control_mouse_speed_title),
                    valueRange = 25f..300f,
                    suffix = "%",
                    fineTuningControl = true,
                    onValueChange = { mouseSpeed = it }
                )

                SliderSettingsLayout(
                    unit = AllSettings.mouseLongPressDelay,
                    title = stringResource(R.string.settings_control_mouse_long_press_delay_title),
                    summary = stringResource(R.string.settings_control_mouse_long_press_delay_summary),
                    valueRange = 100f..1000f,
                    suffix = "ms",
                    fineTuningControl = true,
                    onValueChange = { mouseLongPressDelay = it }
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
                    unit = AllSettings.gestureControl,
                    title = stringResource(R.string.settings_control_gesture_control_title),
                    summary = stringResource(R.string.settings_control_gesture_control_summary),
                    onCheckedChange = { gestureControl = it }
                )

                ListSettingsLayout(
                    unit = AllSettings.gestureTapMouseAction,
                    items = GestureActionType.entries,
                    title = stringResource(R.string.settings_control_gesture_tap_action_title),
                    summary = stringResource(R.string.settings_control_gesture_tap_action_summary),
                    getItemText = { stringResource(it.nameRes) },
                    getItemId = { it.name },
                    enabled = gestureControl,
                    onValueChange = { gestureTapMouseAction = it }
                )

                ListSettingsLayout(
                    unit = AllSettings.gestureLongPressMouseAction,
                    items = GestureActionType.entries,
                    title = stringResource(R.string.settings_control_gesture_long_press_action_title),
                    summary = stringResource(R.string.settings_control_gesture_long_press_action_summary),
                    getItemText = { stringResource(it.nameRes) },
                    getItemId = { it.name },
                    enabled = gestureControl,
                    onValueChange = { gestureLongPressMouseAction = it }
                )

                SliderSettingsLayout(
                    unit = AllSettings.gestureLongPressDelay,
                    title = stringResource(R.string.settings_control_gesture_long_press_delay_title),
                    summary = stringResource(R.string.settings_control_mouse_long_press_delay_summary),
                    valueRange = 100f..1000f,
                    suffix = "ms",
                    enabled = gestureControl,
                    fineTuningControl = true,
                    onValueChange = { gestureLongPressDelay = it }
                )
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
    mouseSize: Int
) {
    val context = LocalContext.current

    var triggerState by remember { mutableIntStateOf(0) }

    var mouseOperation by remember { mutableStateOf<MousePointerOperation>(MousePointerOperation.None) }
    when (mouseOperation) {
        is MousePointerOperation.None -> {}
        is MousePointerOperation.Reset -> {
            SimpleAlertDialog(
                title = stringResource(R.string.generic_reset),
                text = stringResource(R.string.settings_control_mouse_pointer_reset_message),
                onConfirm = {
                    FileUtils.deleteQuietly(mousePointerFile)
                    mouseOperation = MousePointerOperation.Refresh
                },
                onDismiss = { mouseOperation = MousePointerOperation.None }
            )
        }
        is MousePointerOperation.Refresh -> {
            triggerState++
            mouseOperation = MousePointerOperation.None
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
                        mouseOperation = MousePointerOperation.Refresh
                    },
                    onError = { th ->
                        FileUtils.deleteQuietly(mousePointerFile)
                        ObjectStates.updateThrowable(
                            ObjectStates.ThrowableMessage(
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
                        mouseOperation = MousePointerOperation.Reset
                    },
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = stringResource(R.string.generic_reset),
                    text = stringResource(R.string.generic_reset)
                )
            }
        }
    }
}
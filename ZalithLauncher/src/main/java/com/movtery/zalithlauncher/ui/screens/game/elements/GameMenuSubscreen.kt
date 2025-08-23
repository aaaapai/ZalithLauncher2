package com.movtery.zalithlauncher.ui.screens.game.elements

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.GestureActionType
import com.movtery.zalithlauncher.setting.enums.MouseControlMode
import com.movtery.zalithlauncher.ui.components.MenuListLayout
import com.movtery.zalithlauncher.ui.components.MenuSliderLayout
import com.movtery.zalithlauncher.ui.components.MenuState
import com.movtery.zalithlauncher.ui.components.MenuSubscreen
import com.movtery.zalithlauncher.ui.components.MenuSwitchButton
import com.movtery.zalithlauncher.ui.components.MenuTextButton

@Composable
fun GameMenuSubscreen(
    state: MenuState,
    closeScreen: () -> Unit,
    onForceClose: () -> Unit,
    onSwitchLog: () -> Unit,
    onRefreshWindowSize: () -> Unit,
    onInputMethod: () -> Unit
) {
    MenuSubscreen(
        state = state,
        closeScreen = closeScreen
    ) {
        Text(
            modifier = Modifier
                .padding(all = 8.dp)
                .align(Alignment.CenterHorizontally),
            text = stringResource(R.string.game_menu_title),
            style = MaterialTheme.typography.titleMedium
        )
        HorizontalDivider(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val itemCommonModifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()

            //强制关闭
            item {
                MenuTextButton(
                    modifier = itemCommonModifier,
                    text = stringResource(R.string.game_button_force_close),
                    onClick = onForceClose
                )
            }
            //日志输出
            item {
                MenuTextButton(
                    modifier = itemCommonModifier,
                    text = stringResource(R.string.game_menu_option_switch_log),
                    onClick = onSwitchLog
                )
            }
            //切换输入法
            item {
                MenuTextButton(
                    modifier = itemCommonModifier,
                    text = stringResource(R.string.game_menu_option_input_method),
                    onClick = {
                        onInputMethod()
                        closeScreen()
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            //帧率显示
            item {
                MenuSwitchButton(
                    modifier = itemCommonModifier,
                    text = stringResource(R.string.game_menu_option_switch_fps),
                    switch = AllSettings.showFPS.state,
                    onSwitch = { AllSettings.showFPS.save(it) }
                )
            }
            //游戏窗口分辨率
            item {
                MenuSliderLayout(
                    modifier = itemCommonModifier,
                    title = stringResource(R.string.settings_renderer_resolution_scale_title),
                    value = AllSettings.resolutionRatio.state,
                    valueRange = 25f..300f,
                    onValueChange = { value ->
                        AllSettings.resolutionRatio.updateState(value)
                        onRefreshWindowSize()
                    },
                    onValueChangeFinished = { value ->
                        AllSettings.resolutionRatio.save(value)
                        onRefreshWindowSize()
                    },
                    suffix = "%",
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            //隐藏虚拟鼠标
            item {
                MenuSwitchButton(
                    modifier = itemCommonModifier,
                    text = stringResource(R.string.settings_control_mouse_hide_title),
                    switch = AllSettings.hideMouse.state,
                    onSwitch = { AllSettings.hideMouse.save(it) },
                    enabled = AllSettings.mouseControlMode.state == MouseControlMode.CLICK
                )
            }
            //虚拟鼠标大小
            item {
                MenuSliderLayout(
                    modifier = itemCommonModifier,
                    title = stringResource(R.string.settings_control_mouse_size_title),
                    value = AllSettings.mouseSize.state,
                    valueRange = 5f..50f,
                    onValueChange = { AllSettings.mouseSize.updateState(it) },
                    onValueChangeFinished = { AllSettings.mouseSize.save(it) },
                    suffix = "Dp"
                )
            }
            //虚拟鼠标灵敏度
            item {
                MenuSliderLayout(
                    modifier = itemCommonModifier,
                    title = stringResource(R.string.settings_control_mouse_sensitivity_title),
                    value = AllSettings.cursorSensitivity.state,
                    valueRange = 25f..300f,
                    onValueChange = { AllSettings.cursorSensitivity.updateState(it) },
                    onValueChangeFinished = { AllSettings.cursorSensitivity.save(it) },
                    suffix = "%"
                )
            }
            //抓获鼠标滑动灵敏度
            item {
                MenuSliderLayout(
                    modifier = itemCommonModifier,
                    title = stringResource(R.string.settings_control_mouse_capture_sensitivity_title),
                    value = AllSettings.mouseCaptureSensitivity.state,
                    valueRange = 25f..300f,
                    onValueChange = { AllSettings.mouseCaptureSensitivity.updateState(it) },
                    onValueChangeFinished = { AllSettings.mouseCaptureSensitivity.save(it) },
                    suffix = "%"
                )
            }
            //虚拟鼠标长按触发的延迟
            item {
                MenuSliderLayout(
                    modifier = itemCommonModifier,
                    title = stringResource(R.string.settings_control_mouse_long_press_delay_title),
                    value = AllSettings.mouseLongPressDelay.state,
                    valueRange = 100f..1000f,
                    onValueChange = { AllSettings.mouseLongPressDelay.updateState(it) },
                    onValueChangeFinished = { AllSettings.mouseLongPressDelay.save(it) },
                    suffix = "ms"
                )
            }
            //鼠标控制模式
            item {
                MenuListLayout(
                    modifier = itemCommonModifier,
                    title = stringResource(R.string.settings_control_mouse_control_mode_title),
                    items = MouseControlMode.entries,
                    currentItem = AllSettings.mouseControlMode.state,
                    onItemChange = { AllSettings.mouseControlMode.save(it) },
                    getItemText = { stringResource(it.nameRes) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            //手势控制
            item {
                MenuSwitchButton(
                    modifier = itemCommonModifier,
                    text = stringResource(R.string.settings_control_gesture_control_title),
                    switch = AllSettings.gestureControl.state,
                    onSwitch = { AllSettings.gestureControl.save(it) }
                )
            }
            //点击触发的操作类型
            item {
                MenuListLayout(
                    modifier = itemCommonModifier,
                    title = stringResource(R.string.settings_control_gesture_tap_action_title),
                    items = GestureActionType.entries,
                    currentItem = AllSettings.gestureTapMouseAction.state,
                    onItemChange = { AllSettings.gestureTapMouseAction.save(it) },
                    getItemText = { stringResource(it.nameRes) },
                    enabled = AllSettings.gestureControl.state
                )
            }
            //长按触发的操作类型
            item {
                MenuListLayout(
                    modifier = itemCommonModifier,
                    title = stringResource(R.string.settings_control_gesture_long_press_action_title),
                    items = GestureActionType.entries,
                    currentItem = AllSettings.gestureLongPressMouseAction.state,
                    onItemChange = { AllSettings.gestureLongPressMouseAction.save(it) },
                    getItemText = { stringResource(it.nameRes) },
                    enabled = AllSettings.gestureControl.state
                )
            }
            //手势长按触发的延迟
            item {
                MenuSliderLayout(
                    modifier = itemCommonModifier,
                    title = stringResource(R.string.settings_control_gesture_long_press_delay_title),
                    value = AllSettings.gestureLongPressDelay.state,
                    valueRange = 100f..1000f,
                    enabled = AllSettings.gestureControl.state,
                    onValueChange = { AllSettings.gestureLongPressDelay.updateState(it) },
                    onValueChangeFinished = { AllSettings.gestureLongPressDelay.save(it) },
                    suffix = "ms"
                )
            }
        }
    }
}
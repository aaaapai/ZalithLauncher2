package com.movtery.zalithlauncher.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.bridge.ZLBridgeStates
import com.movtery.zalithlauncher.game.input.LWJGLCharSender
import com.movtery.zalithlauncher.game.keycodes.LwjglGlfwKeycode
import com.movtery.zalithlauncher.game.support.touch_controller.touchControllerInputModifier
import com.movtery.zalithlauncher.game.support.touch_controller.touchControllerTouchModifier
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.toAction
import com.movtery.zalithlauncher.ui.components.MenuState
import com.movtery.zalithlauncher.ui.control.input.TextInputMode
import com.movtery.zalithlauncher.ui.control.input.textInputHandler
import com.movtery.zalithlauncher.ui.control.mouse.SwitchableMouseLayout
import com.movtery.zalithlauncher.ui.screens.game.elements.DraggableGameBall
import com.movtery.zalithlauncher.ui.screens.game.elements.ForceCloseOperation
import com.movtery.zalithlauncher.ui.screens.game.elements.GameMenuSubscreen
import com.movtery.zalithlauncher.ui.screens.game.elements.LogBox
import com.movtery.zalithlauncher.ui.screens.game.elements.LogState
import com.movtery.zalithlauncher.viewmodel.EventViewModel
import kotlinx.coroutines.flow.filterIsInstance
import org.lwjgl.glfw.CallbackBridge

@Composable
fun GameScreen(
    version: Version,
    isGameRendering: Boolean,
    logState: LogState,
    onLogStateChange: (LogState) -> Unit = {},
    isTouchProxyEnabled: Boolean,
    onInputAreaRectUpdated: (IntRect?) -> Unit = {},
    eventViewModel: EventViewModel
) {
    //游戏菜单状态
    var gameMenuState by remember { mutableStateOf(MenuState.NONE) }

    //游戏菜单
    var forceCloseState by remember { mutableStateOf<ForceCloseOperation>(ForceCloseOperation.None) }
    var textInputMode by remember { mutableStateOf(TextInputMode.DISABLE) }

    ForceCloseOperation(
        operation = forceCloseState,
        onChange = { forceCloseState = it },
        text = stringResource(R.string.game_menu_option_force_close_text)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        GameInfoBox(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(all = 16.dp),
            version = version,
            isGameRendering = isGameRendering
        )

        MouseControlLayout(
            isTouchProxyEnabled = isTouchProxyEnabled,
            modifier = Modifier.fillMaxSize(),
            onInputAreaRectUpdated = onInputAreaRectUpdated,
            textInputMode = textInputMode,
            onCloseInputMethod = { textInputMode = TextInputMode.DISABLE }
        )

        LogBox(
            enableLog = logState.value,
            modifier = Modifier.fillMaxSize()
        )

        GameMenuSubscreen(
            state = gameMenuState,
            closeScreen = { gameMenuState = MenuState.HIDE },
            onForceClose = { forceCloseState = ForceCloseOperation.Show },
            onSwitchLog = { onLogStateChange(logState.next()) },
            onRefreshWindowSize = { eventViewModel.sendEvent(EventViewModel.Event.Game.RefreshSize) },
            onInputMethod = { textInputMode = textInputMode.switch() }
        )

        DraggableGameBall(
            showGameFps = AllSettings.showFPS.state,
            onClick = {
                gameMenuState = gameMenuState.next()
            }
        )
    }

    LaunchedEffect(Unit) {
        eventViewModel.events
            .filterIsInstance<EventViewModel.Event.Game.ShowIme>()
            .collect {
                textInputMode = TextInputMode.ENABLE
            }
    }
}

@Composable
private fun GameInfoBox(
    modifier: Modifier = Modifier,
    version: Version,
    isGameRendering: Boolean
) {
    AnimatedVisibility(
        visible = !isGameRendering,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterVertically)
                )

                //提示信息
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.game_loading),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.game_loading_version_name, version.getVersionName()),
                        style = MaterialTheme.typography.labelLarge
                    )
                    version.getVersionInfo()?.let { info ->
                        Text(
                            text = stringResource(R.string.game_loading_version_info, info.getInfoString()),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MouseControlLayout(
    isTouchProxyEnabled: Boolean,
    modifier: Modifier = Modifier,
    onInputAreaRectUpdated: (IntRect?) -> Unit = {},
    textInputMode: TextInputMode,
    onCloseInputMethod: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .then(
                if (isTouchProxyEnabled) {
                    Modifier
                        .touchControllerTouchModifier()
                        .touchControllerInputModifier(
                            onInputAreaRectUpdated = onInputAreaRectUpdated,
                        )
                } else Modifier
            )
            .textInputHandler(
                mode = textInputMode,
                sender = LWJGLCharSender,
                onCloseInputMethod = onCloseInputMethod
            )
    ) {

        val capturedSpeedFactor = AllSettings.mouseCaptureSensitivity.state / 100f
        val capturedTapMouseAction = AllSettings.gestureTapMouseAction.state.toAction()
        val capturedLongPressMouseAction = AllSettings.gestureLongPressMouseAction.state.toAction()

        SwitchableMouseLayout(
            modifier = Modifier.fillMaxSize(),
            cursorMode = ZLBridgeStates.cursorMode,
            onMouseTap = { position ->
                CallbackBridge.putMouseEventWithCoords(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), position.x.sumPosition(), position.y.sumPosition())
            },
            onCapturedTap = { position ->
                if (AllSettings.gestureControl.state) {
                    CallbackBridge.putMouseEvent(capturedTapMouseAction)
                }
            },
            onLongPress = {
                CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), true)
            },
            onLongPressEnd = {
                CallbackBridge.putMouseEvent(LwjglGlfwKeycode.GLFW_MOUSE_BUTTON_LEFT.toInt(), false)
            },
            onCapturedLongPress = {
                if (AllSettings.gestureControl.state) {
                    CallbackBridge.putMouseEvent(capturedLongPressMouseAction, true)
                }
            },
            onCapturedLongPressEnd = {
                if (AllSettings.gestureControl.state) {
                    CallbackBridge.putMouseEvent(capturedLongPressMouseAction, false)
                }
            },
            onPointerMove = { pos ->
                pos.sendPosition()
            },
            onCapturedMove = { delta ->
                CallbackBridge.sendCursorDelta(
                    delta.x * capturedSpeedFactor,
                    delta.y * capturedSpeedFactor
                )
            },
            onMouseScroll = { scroll ->
                CallbackBridge.sendScroll(scroll.x.toDouble(), scroll.y.toDouble())
            },
            onMouseButton = { button, pressed ->
                val code = LWJGLCharSender.getMouseButton(button) ?: return@SwitchableMouseLayout
                CallbackBridge.sendMouseButton(code.toInt(), pressed)
            },
        )
    }
}

private fun Offset.sendPosition() {
    CallbackBridge.sendCursorPos(x.sumPosition(), y.sumPosition())
}

private fun Float.sumPosition(): Float {
    return (this * (AllSettings.resolutionRatio.state / 100f))
}
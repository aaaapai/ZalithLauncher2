package com.movtery.zalithlauncher.ui.screens.content.settings

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.state.LocalColorThemeState
import com.movtery.zalithlauncher.state.LocalCustomColorThemeState
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.state.getCustomColorFromSettings
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.base.FullScreenComponentActivity
import com.movtery.zalithlauncher.ui.components.ColorPickerDialog
import com.movtery.zalithlauncher.ui.screens.content.SETTINGS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.ui.theme.ColorThemeType
import com.movtery.zalithlauncher.utils.animation.TransitionAnimationType
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState

const val LAUNCHER_SETTINGS_TAG = "LauncherSettingsScreen"

private sealed interface CustomColorOperation {
    data object None : CustomColorOperation
    /** 展示自定义主题颜色 Dialog */
    data object Dialog: CustomColorOperation
}

@Composable
fun LauncherSettingsScreen() {
    val context = LocalContext.current

    BaseScreen(
        parentScreenTag = SETTINGS_SCREEN_TAG,
        parentCurrentTag = MutableStates.mainScreenTag,
        childScreenTag = LAUNCHER_SETTINGS_TAG,
        childCurrentTag = MutableStates.settingsScreenTag
    ) { isVisible ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
                .padding(all = 12.dp)
        ) {
            val currentColorThemeState = LocalColorThemeState.current

            val yOffset1 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible
            )

            SettingsBackground(
                modifier = Modifier.offset { IntOffset(x = 0, y = yOffset1.roundToPx()) }
            ) {
                var customColorOperation by remember { mutableStateOf<CustomColorOperation>(CustomColorOperation.None) }
                CustomColorOperation(
                    customColorOperation = customColorOperation,
                    updateOperation = { customColorOperation = it }
                )

                EnumSettingsLayout(
                    unit = AllSettings.launcherColorTheme,
                    title = stringResource(R.string.settings_launcher_color_theme_title),
                    summary = stringResource(R.string.settings_launcher_color_theme_summary),
                    entries = ColorThemeType.entries,
                    getRadioEnable = { enum ->
                        if (enum == ColorThemeType.DYNAMIC) Build.VERSION.SDK_INT >= Build.VERSION_CODES.S else true
                    },
                    getRadioText = { enum ->
                        when (enum) {
                            ColorThemeType.DYNAMIC -> stringResource(R.string.theme_color_dynamic)
                            ColorThemeType.EMBERMIRE -> stringResource(R.string.theme_color_embermire)
                            ColorThemeType.VELVET_ROSE -> stringResource(R.string.theme_color_velvet_rose)
                            ColorThemeType.MISTWAVE -> stringResource(R.string.theme_color_mistwave)
                            ColorThemeType.GLACIER -> stringResource(R.string.theme_color_glacier)
                            ColorThemeType.VERDANTFIELD -> stringResource(R.string.theme_color_verdant_field)
                            ColorThemeType.URBAN_ASH -> stringResource(R.string.theme_color_urban_ash)
                            ColorThemeType.VERDANT_DAWN -> stringResource(R.string.theme_color_verdant_dawn)
                            ColorThemeType.CUSTOM -> stringResource(R.string.generic_custom)
                        }
                    },
                    onRadioClick = { enum ->
                        if (enum == ColorThemeType.CUSTOM) customColorOperation = CustomColorOperation.Dialog
                    }
                ) { type ->
                    currentColorThemeState.updateValue(type)
                }

                SwitchSettingsLayout(
                    unit = AllSettings.launcherFullScreen,
                    title = stringResource(R.string.settings_launcher_full_screen_title),
                    summary = stringResource(R.string.settings_launcher_full_screen_summary),
                    onCheckedChange = {
                        val activity = context as? FullScreenComponentActivity
                        activity?.fullScreenViewModel?.triggerRefresh()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            val yOffset2 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible,
                delayMillis = 50
            )

            //动画设置板块
            SettingsBackground(
                modifier = Modifier.offset { IntOffset(x = 0, y = yOffset2.roundToPx()) }
            ) {
                SliderSettingsLayout(
                    unit = AllSettings.launcherAnimateSpeed,
                    title = stringResource(R.string.settings_launcher_animate_speed_title),
                    summary = stringResource(R.string.settings_launcher_animate_speed_summary),
                    valueRange = 0f..10f,
                    steps = 9,
                    suffix = "x"
                )

                SliderSettingsLayout(
                    unit = AllSettings.launcherAnimateExtent,
                    title = stringResource(R.string.settings_launcher_animate_extent_title),
                    summary = stringResource(R.string.settings_launcher_animate_extent_summary),
                    valueRange = 0f..10f,
                    steps = 9,
                    suffix = "x"
                )

                EnumSettingsLayout(
                    unit = AllSettings.launcherSwapAnimateType,
                    title = stringResource(R.string.settings_launcher_swap_animate_type_title),
                    summary = stringResource(R.string.settings_launcher_swap_animate_type_summary),
                    entries = TransitionAnimationType.entries,
                    getRadioEnable = { true },
                    getRadioText = { enum ->
                        when (enum) {
                            TransitionAnimationType.CLOSE -> stringResource(R.string.generic_close)
                            TransitionAnimationType.BOUNCE -> stringResource(R.string.animate_type_bounce)
                            TransitionAnimationType.JELLY_BOUNCE -> stringResource(R.string.animate_type_jelly_bounce)
                            TransitionAnimationType.SLICE_IN -> stringResource(R.string.animate_type_slice_in)
                        }
                    }
                ) { type ->
                    MutableStates.launcherAnimateType = type
                }
            }
        }
    }
}

@Composable
private fun CustomColorOperation(
    customColorOperation: CustomColorOperation,
    updateOperation: (CustomColorOperation) -> Unit
) {
    when (customColorOperation) {
        is CustomColorOperation.None -> {}
        is CustomColorOperation.Dialog -> {
            val customColorTheme = LocalCustomColorThemeState.current
            ColorPickerDialog(
                initialColor = getCustomColorFromSettings(),
                realTimeUpdate = false,
                onColorChanged = { color ->
                    customColorTheme.updateValue(color)
                },
                onDismissRequest = {
                    updateOperation(CustomColorOperation.None)
                },
                onConfirm = { color ->
                    customColorTheme.updateValue(color)
                    customColorTheme.saveValue()
                    updateOperation(CustomColorOperation.None)
                },
                showAlpha = false,
                showBrightness = false
            )
        }
    }
}
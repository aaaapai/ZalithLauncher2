package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.screens.content.SettingsScreenKey
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.ui.screens.content.settingsScreenKey
import com.movtery.zalithlauncher.ui.screens.main.elements.mainScreenKey
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.platform.MemoryUtils.getMaxMemoryForSettings
import kotlinx.serialization.Serializable

@Serializable
data object GameSettingsScreenKey: NavKey

@Composable
fun GameSettingsScreen() {
    BaseScreen(
        Triple(SettingsScreenKey, mainScreenKey, false),
        Triple(GameSettingsScreenKey, settingsScreenKey, false)
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
                    unit = AllSettings.versionIsolation,
                    title = stringResource(R.string.settings_game_version_isolation_title),
                    summary = stringResource(R.string.settings_game_version_isolation_summary)
                )

                SwitchSettingsLayout(
                    unit = AllSettings.skipGameIntegrityCheck,
                    title = stringResource(R.string.settings_game_skip_game_integrity_check_title),
                    summary = stringResource(R.string.settings_game_skip_game_integrity_check_summary)
                )

                TextInputSettingsLayout(
                    unit = AllSettings.versionCustomInfo,
                    title = stringResource(R.string.settings_game_version_custom_info_title),
                    summary = stringResource(R.string.settings_game_version_custom_info_summary)
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
                ListSettingsLayout(
                    unit = AllSettings.javaRuntime,
                    items = RuntimesManager.getRuntimes().filter { it.isCompatible },
                    title = stringResource(R.string.settings_game_java_runtime_title),
                    summary = stringResource(R.string.settings_game_java_runtime_summary),
                    getItemText = { it.name },
                    getItemId = { it.name }
                )

                SwitchSettingsLayout(
                    unit = AllSettings.autoPickJavaRuntime,
                    title = stringResource(R.string.settings_game_auto_pick_java_runtime_title),
                    summary = stringResource(R.string.settings_game_auto_pick_java_runtime_summary)
                )

                SliderSettingsLayout(
                    unit = AllSettings.ramAllocation,
                    title = stringResource(R.string.settings_game_java_memory_title),
                    summary = stringResource(R.string.settings_game_java_memory_summary),
                    valueRange = 256f..getMaxMemoryForSettings(LocalContext.current).toFloat(),
                    suffix = "MB",
                    fineTuningControl = true
                )

                TextInputSettingsLayout(
                    unit = AllSettings.jvmArgs,
                    title = stringResource(R.string.settings_game_jvm_args_title),
                    summary = stringResource(R.string.settings_game_jvm_args_summary)
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
                SliderSettingsLayout(
                    unit = AllSettings.logTextSize,
                    title = stringResource(R.string.settings_game_log_text_size_title),
                    summary = stringResource(R.string.settings_game_log_text_size_summary),
                    valueRange = 5f..20f,
                    suffix = "Sp",
                    fineTuningControl = true
                )

                SliderSettingsLayout(
                    unit = AllSettings.logBufferFlushInterval,
                    title = stringResource(R.string.settings_game_log_buffer_flush_interval_title),
                    summary = stringResource(R.string.settings_game_log_buffer_flush_interval_summary),
                    valueRange = 100f..1000f,
                    suffix = "ms",
                    fineTuningControl = true
                )
            }
        }
    }
}
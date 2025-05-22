package com.movtery.zalithlauncher.ui.screens.content.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.plugin.driver.Driver
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.scaleFactor
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.SimpleAlertDialog
import com.movtery.zalithlauncher.ui.components.SwitchLayout
import com.movtery.zalithlauncher.ui.screens.content.SETTINGS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.layouts.SettingsBackground
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.device.checkVulkanSupport
import com.movtery.zalithlauncher.utils.isAdrenoGPU

const val RENDERER_SETTINGS_SCREEN_TAG = "RendererSettingsScreen"

@Composable
fun RendererSettingsScreen() {
    BaseScreen(
        parentScreenTag = SETTINGS_SCREEN_TAG,
        parentCurrentTag = MutableStates.mainScreenTag,
        childScreenTag = RENDERER_SETTINGS_SCREEN_TAG,
        childCurrentTag = MutableStates.settingsScreenTag
    ) { isVisible ->
        val context = LocalContext.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
                .padding(all = 12.dp)
        ) {
            val yOffset1 by swapAnimateDpAsState(
                targetValue =  (-40).dp,
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
                ListSettingsLayout(
                    unit = AllSettings.renderer,
                    items = Renderers.getCompatibleRenderers(context).second,
                    title = stringResource(R.string.settings_renderer_global_renderer_title),
                    summary = stringResource(R.string.settings_renderer_global_renderer_summary),
                    getItemText = { it.getRendererName() },
                    getItemId = { it.getUniqueIdentifier() },
                    getItemSummary = {
                        RendererSummaryLayout(it)
                    }
                )

                ListSettingsLayout(
                    unit = AllSettings.vulkanDriver,
                    items = DriverPluginManager.getDriverList(),
                    title = stringResource(R.string.settings_renderer_global_vulkan_driver_title),
                    getItemText = { it.name },
                    getItemId = { it.id },
                    getItemSummary = {
                        DriverSummaryLayout(it)
                    }
                )

                SliderSettingsLayout(
                    unit = AllSettings.resolutionRatio,
                    title = stringResource(R.string.settings_renderer_resolution_scale_title),
                    summary = stringResource(R.string.settings_renderer_resolution_scale_summary),
                    valueRange = 25f..300f,
                    suffix = "%",
                    fineTuningControl = true,
                    onValueChange = { scaleFactor = it / 100f }
                )

                SwitchSettingsLayout(
                    unit = AllSettings.gameFullScreen,
                    title = stringResource(R.string.settings_renderer_full_screen_title),
                    summary = stringResource(R.string.settings_renderer_full_screen_summary)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            val yOffset2 by swapAnimateDpAsState(
                targetValue =  (-40).dp,
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
                    unit = AllSettings.sustainedPerformance,
                    title = stringResource(R.string.settings_renderer_sustained_performance_title),
                    summary = stringResource(R.string.settings_renderer_sustained_performance_summary)
                )

                if (checkVulkanSupport(LocalContext.current.packageManager)) {
                    var adrenoGPUAlert by remember { mutableStateOf(false) }

                    var value by remember { mutableStateOf(AllSettings.zinkPreferSystemDriver.getValue()) }

                    fun change(value1: Boolean) {
                        value = value1
                        AllSettings.zinkPreferSystemDriver.put(value).save()
                    }

                    SwitchLayout(
                        title = stringResource(R.string.settings_renderer_vulkan_driver_system_title),
                        summary = stringResource(R.string.settings_renderer_vulkan_driver_system_summary),
                        checked = value,
                        onCheckedChange = { checked ->
                            if (checked && isAdrenoGPU()) adrenoGPUAlert = true
                            else change(checked)
                        }
                    )

                    if (adrenoGPUAlert) {
                        SimpleAlertDialog(
                            title = stringResource(R.string.generic_warning),
                            text = stringResource(R.string.settings_renderer_zink_driver_adreno),
                            onConfirm = {
                                change(true)
                                adrenoGPUAlert = false
                            },
                            onDismiss = {
                                change(false)
                                adrenoGPUAlert = false
                            }
                        )
                    }
                }

                SwitchSettingsLayout(
                    unit = AllSettings.vsyncInZink,
                    title = stringResource(R.string.settings_renderer_vsync_in_zink_title),
                    summary = stringResource(R.string.settings_renderer_vsync_in_zink_summary)
                )

                SwitchSettingsLayout(
                    unit = AllSettings.bigCoreAffinity,
                    title = stringResource(R.string.settings_renderer_force_big_core_title),
                    summary = stringResource(R.string.settings_renderer_force_big_core_summary)
                )

                SwitchSettingsLayout(
                    unit = AllSettings.dumpShaders,
                    title = stringResource(R.string.settings_renderer_shader_dump_title),
                    summary = stringResource(R.string.settings_renderer_shader_dump_summary)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun RendererSummaryLayout(renderer: RendererInterface) {
    FlowRow(modifier = Modifier.alpha(0.7f)) {
        with(renderer) {
            getRendererSummary()?.let { summary ->
                Text(text = summary, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(12.dp))
            }

            val minVer = getMinMCVersion()
            val maxVer = getMaxMCVersion()

            if (minVer != null || maxVer != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = stringResource(R.string.renderer_version_support), style = MaterialTheme.typography.labelSmall)

                    minVer?.let {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = ">= $it", style = MaterialTheme.typography.labelSmall)
                    }

                    maxVer?.let {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "<= $it", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun DriverSummaryLayout(driver: Driver) {
    with(driver) {
        summary?.let { text ->
            Text(
                modifier = Modifier.alpha(0.7f),
                text = text, style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
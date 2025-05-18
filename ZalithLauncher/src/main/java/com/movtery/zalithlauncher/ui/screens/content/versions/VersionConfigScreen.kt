package com.movtery.zalithlauncher.ui.screens.content.versions

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.multirt.RuntimesManager
import com.movtery.zalithlauncher.game.plugin.driver.DriverPluginManager
import com.movtery.zalithlauncher.game.renderer.Renderers
import com.movtery.zalithlauncher.game.version.installed.VersionConfig
import com.movtery.zalithlauncher.game.version.installed.VersionsManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.state.MutableStates
import com.movtery.zalithlauncher.state.ObjectStates
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.IDItem
import com.movtery.zalithlauncher.ui.components.SimpleIDListLayout
import com.movtery.zalithlauncher.ui.components.SimpleIntSliderLayout
import com.movtery.zalithlauncher.ui.components.SimpleListLayout
import com.movtery.zalithlauncher.ui.components.TextInputLayout
import com.movtery.zalithlauncher.ui.screens.content.VERSION_SETTINGS_SCREEN_TAG
import com.movtery.zalithlauncher.ui.screens.content.settings.RendererSummaryLayout
import com.movtery.zalithlauncher.ui.screens.content.versions.layouts.VersionSettingsBackground
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState
import com.movtery.zalithlauncher.utils.platform.MemoryUtils
import com.movtery.zalithlauncher.utils.string.StringUtils.Companion.getMessageOrToString

const val VERSION_CONFIG_SCREEN_TAG = "VersionConfigScreen"

@Composable
fun VersionConfigScreen() {
    BaseScreen(
        parentScreenTag = VERSION_SETTINGS_SCREEN_TAG,
        parentCurrentTag = MutableStates.mainScreenTag,
        childScreenTag = VERSION_CONFIG_SCREEN_TAG,
        childCurrentTag = MutableStates.versionSettingsScreenTag
    ) { isVisible ->

        val config = VersionsManager.versionBeingSet?.takeIf { it.isValid() }?.getVersionConfig() ?: run {
            ObjectStates.backToLauncherScreen()
            return@BaseScreen
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
                .padding(all = 12.dp)
        ) {
            val yOffset1 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible
            )

            VersionConfigs(
                config = config,
                modifier = Modifier.offset { IntOffset(x = 0, y = yOffset1.roundToPx()) }
            )

            Spacer(modifier = Modifier.height(12.dp))
            val yOffset2 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible,
                delayMillis = 50
            )

            GameConfigs(
                config = config,
                modifier = Modifier.offset { IntOffset(x = 0, y = yOffset2.roundToPx()) }
            )

            Spacer(modifier = Modifier.height(12.dp))
            val yOffset3 by swapAnimateDpAsState(
                targetValue = (-40).dp,
                swapIn = isVisible,
                delayMillis = 100
            )

            SupportConfigs(
                config = config,
                modifier = Modifier.offset { IntOffset(x = 0, y = yOffset3.roundToPx()) }
            )
        }
    }
}

@Composable
private fun VersionConfigs(
    config: VersionConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    VersionSettingsBackground(modifier = modifier) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 4.dp, bottom = 8.dp),
            text = stringResource(R.string.versions_config_version_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        StatefulSwitchLayoutFollowGlobal(
            currentValue = config.isolationType,
            onValueChange = { type ->
                if (config.isolationType != type) {
                    config.isolationType = type
                    config.saveOrShowError(context)
                }
            },
            title = stringResource(R.string.versions_config_isolation_title),
            summary = stringResource(R.string.versions_config_isolation_summary)
        )

        StatefulSwitchLayoutFollowGlobal(
            currentValue = config.skipGameIntegrityCheck,
            onValueChange = { type ->
                if (config.skipGameIntegrityCheck != type) {
                    config.skipGameIntegrityCheck = type
                    config.saveOrShowError(context)
                }
            },
            title = stringResource(R.string.settings_game_skip_game_integrity_check_title),
            summary = stringResource(R.string.settings_game_skip_game_integrity_check_summary)
        )

        val renderers = Renderers.getCompatibleRenderers(context).second
        val renderersIdList = getIDList(renderers) { IDItem(it.getUniqueIdentifier(), it.getRendererName()) }
        SimpleListLayout(
            items = renderersIdList,
            currentId = config.renderer,
            defaultId = "",
            title = stringResource(R.string.versions_config_renderer),
            getItemText = { it.title },
            getItemId = { it.id },
            getItemSummary = { item ->
                renderers.find { it.getUniqueIdentifier() == item.id }?.let { renderer ->
                    RendererSummaryLayout(renderer)
                }
            },
            onValueChange = { item ->
                if (config.renderer != item.id) {
                    config.renderer = item.id
                    config.saveOrShowError(context)
                }
            }
        )

        SimpleIDListLayout(
            items = getIDList(DriverPluginManager.getDriverList()) { IDItem(it.id, it.name) },
            currentId = config.driver,
            defaultId = "",
            title = stringResource(R.string.versions_config_vulkan_driver),
            onValueChange = { item ->
                if (config.driver != item.id) {
                    config.driver = item.id
                    config.saveOrShowError(context)
                }
            }
        )
    }
}

@Composable
private fun GameConfigs(
    config: VersionConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    VersionSettingsBackground(modifier = modifier) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 4.dp, bottom = 8.dp),
            text = stringResource(R.string.versions_config_game_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        SimpleIDListLayout(
            items = getIDList(RuntimesManager.getRuntimes().filter { it.isCompatible }) { IDItem(it.name, it.name) },
            currentId = config.javaRuntime,
            defaultId = "",
            title = stringResource(R.string.settings_game_java_runtime_title),
            summary = stringResource(R.string.versions_config_java_runtime_summary),
            onValueChange = { item ->
                if (config.javaRuntime != item.id) {
                    config.javaRuntime = item.id
                    config.saveOrShowError(context)
                }
            }
        )

        ToggleableSliderSetting(
            currentValue = config.ramAllocation,
            valueRange = 256f..MemoryUtils.getMaxMemoryForSettings(LocalContext.current).toFloat(),
            defaultValue = AllSettings.ramAllocation.getValue(),
            title = stringResource(R.string.settings_game_java_memory_title),
            summary = stringResource(R.string.settings_game_java_memory_summary),
            suffix = "MB",
            onValueChange = { config.ramAllocation = it },
            onValueChangeFinished = { config.saveOrShowError(context) }
        )

        TextInputLayout(
            currentValue = config.customInfo,
            title = stringResource(R.string.settings_game_version_custom_info_title),
            summary = stringResource(R.string.settings_game_version_custom_info_summary),
            onValueChange = { value ->
                if (config.customInfo != value) {
                    config.customInfo = value
                    config.saveOrShowError(context)
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_follow_global_if_blank))
            }
        )

        TextInputLayout(
            currentValue = config.jvmArgs,
            title = stringResource(R.string.settings_game_jvm_args_title),
            summary = stringResource(R.string.settings_game_jvm_args_summary),
            onValueChange = { value ->
                if (config.jvmArgs != value) {
                    config.jvmArgs = value
                    config.saveOrShowError(context)
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_follow_global_if_blank))
            }
        )

        TextInputLayout(
            currentValue = config.serverIp,
            title = stringResource(R.string.versions_config_auto_join_server_ip_title),
            summary = stringResource(R.string.versions_config_auto_join_server_ip_summary),
            onValueChange = { value ->
                if (config.serverIp != value) {
                    config.serverIp = value
                    config.saveOrShowError(context)
                }
            },
            label = {
                Text(text = stringResource(R.string.versions_config_disable_if_blank))
            }
        )
    }
}

@Composable
private fun SupportConfigs(
    config: VersionConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    VersionSettingsBackground(modifier = modifier) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp).padding(top = 4.dp, bottom = 8.dp),
            text = stringResource(R.string.versions_config_support_settings),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        var enableTouchProxy by remember { mutableStateOf(config.enableTouchProxy) }

        SwitchConfigLayout(
            currentValue = enableTouchProxy,
            onCheckedChange = { value ->
                enableTouchProxy = value
                if (config.enableTouchProxy != value) {
                    config.enableTouchProxy = value
                    config.saveOrShowError(context)
                }
            },
            title = stringResource(R.string.versions_config_enable_touch_proxy_title),
            summary = stringResource(R.string.versions_config_enable_touch_proxy_summary)
        )

        var touchVibrateDuration by remember { mutableIntStateOf(config.touchVibrateDuration) }

        SimpleIntSliderLayout(
            value = touchVibrateDuration,
            title = stringResource(R.string.versions_config_vibrate_duration_title),
            summary = stringResource(R.string.versions_config_vibrate_duration_summary),
            valueRange = 80f..500f,
            onValueChange = {
                touchVibrateDuration = it
                config.touchVibrateDuration = touchVibrateDuration
            },
            onValueChangeFinished = {
                config.saveOrShowError(context)
            },
            suffix = "ms",
            fineTuningControl = true
        )
    }
}

@Composable
private fun <E> getIDList(list: List<E>, toIDItem: (E) -> IDItem): List<IDItem> {
    return list.map {
        toIDItem(it)
    }.toMutableList().apply {
        add(0, IDItem("", stringResource(R.string.generic_follow_global)))
    }
}

private fun VersionConfig.saveOrShowError(context: Context) {
    runCatching {
        saveWithThrowable()
    }.onFailure { e ->
        Log.e(VERSION_CONFIG_SCREEN_TAG, "Failed to save version config!", e)
        ObjectStates.updateThrowable(
            ObjectStates.ThrowableMessage(
                title = context.getString(R.string.versions_config_failed_to_save),
                message = e.getMessageOrToString()
            )
        )
    }
}
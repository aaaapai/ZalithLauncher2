package com.movtery.zalithlauncher.ui.screens.content.versions.layouts

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.game.version.installed.SettingState
import com.movtery.zalithlauncher.game.version.installed.Version
import com.movtery.zalithlauncher.ui.components.SimpleIntSliderLayout
import com.movtery.zalithlauncher.ui.components.SwitchLayout
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.ui.screens.content.elements.VersionIconImage

@DslMarker
annotation class VersionSettingsLayoutDsl

@VersionSettingsLayoutDsl
class VersionSettingsLayoutScope {

    @Composable
    fun VersionOverviewItem(
        modifier: Modifier = Modifier,
        version: Version,
        versionName: String = version.getVersionName(),
        versionSummary: String,
        refreshKey: Any? = null
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VersionIconImage(
                version = version,
                modifier = Modifier.size(34.dp),
                refreshKey = refreshKey
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    overflow = TextOverflow.Clip,
                    maxLines = 1,
                    text = versionName,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                    overflow = TextOverflow.Clip,
                    maxLines = 1,
                    text = versionSummary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }

    @Composable
    fun StatefulSwitchLayoutFollowGlobal(
        modifier: Modifier = Modifier,
        currentValue: SettingState,
        onValueChange: (SettingState) -> Unit,
        title: String,
        summary: String? = null
    ) {
        var value by remember { mutableStateOf(currentValue) }

        SwitchLayoutFollowGlobal(
            modifier = modifier,
            value = value,
            onValueChange = {
                value = it
                onValueChange(it)
            },
            title = title,
            summary = summary
        )
    }

    @Composable
    fun SwitchLayoutFollowGlobal(
        modifier: Modifier = Modifier,
        value: SettingState,
        onValueChange: (SettingState) -> Unit = {},
        title: String,
        summary: String? = null
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape = MaterialTheme.shapes.extraLarge)
                .padding(all = 8.dp)
                .padding(bottom = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                TitleAndSummary(title, summary)
            }

            val allItems = SettingState.entries
            var selectedTab by remember { mutableIntStateOf(allItems.indexOf(value)) }

            Surface(
                modifier = Modifier
                    .width(240.dp)
                    .align(Alignment.CenterVertically),
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 1.dp
            ) {
                TabRow(
                    modifier = Modifier.fillMaxWidth(),
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    indicator = @Composable { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.PrimaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab])
                            )
                        }
                    },
                    divider = { /* 禁用底部横线 */ },
                    tabs = {
                        repeat(allItems.size) { index ->
                            val state = allItems[index]
                            Tab(
                                selected = value == state,
                                onClick = {
                                    selectedTab = index
                                    onValueChange(state)
                                }
                            ) {
                                Text(
                                    modifier = Modifier.padding(all = 12.dp),
                                    text = stringResource(state.textRes),
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun SwitchConfigLayout(
        modifier: Modifier = Modifier,
        currentValue: Boolean,
        onCheckedChange: (Boolean) -> Unit = {},
        title: String,
        summary: String? = null
    ) {
        var checked by rememberSaveable { mutableStateOf(currentValue) }

        fun change(value: Boolean) {
            checked = value
            onCheckedChange(checked)
        }

        SwitchLayout(
            checked = checked,
            onCheckedChange = { value ->
                change(value)
            },
            modifier = modifier,
            title = title,
            summary = summary
        )
    }

    @Composable
    fun ToggleableSliderSetting(
        currentValue: Int,
        valueRange: ClosedFloatingPointRange<Float>,
        defaultValue: Int,
        title: String,
        summary: String? = null,
        suffix: String? = null,
        enabled: Boolean = true,
        onValueChange: (Int) -> Unit = {},
        onValueChangeFinished: () -> Unit = {}
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            var checked by remember { mutableStateOf(currentValue >= valueRange.start) }
            var value by remember { mutableIntStateOf(currentValue.takeIf { it >= valueRange.start } ?: defaultValue) }

            if (!enabled) checked = false

            SimpleIntSliderLayout(
                modifier = Modifier.weight(1f),
                value = value,
                title = title,
                summary = summary,
                valueRange = valueRange,
                onValueChange = {
                    value = it
                    onValueChange(value)
                },
                onValueChangeFinished = onValueChangeFinished,
                suffix = suffix,
                enabled = checked,
                fineTuningControl = true,
                appendContent = {
                    Checkbox(
                        modifier = Modifier.padding(start = 12.dp),
                        checked = checked,
                        enabled = enabled,
                        onCheckedChange = {
                            checked = it
                            value = defaultValue
                            onValueChange(if (checked) value else -1)
                            onValueChangeFinished()
                        }
                    )
                }
            )
        }
    }
}
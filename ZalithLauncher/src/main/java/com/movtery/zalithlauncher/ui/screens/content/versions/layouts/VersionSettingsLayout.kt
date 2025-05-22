package com.movtery.zalithlauncher.ui.screens.content.versions.layouts

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
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
    fun StatefulDropdownMenuFollowGlobal(
        modifier: Modifier = Modifier,
        currentValue: SettingState,
        onValueChange: (SettingState) -> Unit,
        iconSize: Dp = 20.dp,
        shape: Shape = RoundedCornerShape(22.0.dp),
        title: String,
        summary: String? = null
    ) {
        var value by remember { mutableStateOf(currentValue) }

        var expanded by remember { mutableStateOf(false) }

        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape = shape)
                .clickable { expanded = !expanded }
                .padding(all = 8.dp)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                TitleAndSummary(title, summary)
            }

            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(value.textRes),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.size(34.dp),
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        modifier = Modifier.size(iconSize),
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.generic_setting)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 3.dp,
                ) {
                    val allEntries = SettingState.entries
                    repeat(allEntries.size) { index ->
                        val state = allEntries[index]
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(state.textRes))
                            },
                            onClick = {
                                value = state
                                onValueChange(value)
                                expanded = false
                            }
                        )
                    }
                }
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
package com.movtery.zalithlauncher.ui.screens.content.settings.layouts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.unit.BooleanSettingUnit
import com.movtery.zalithlauncher.setting.unit.IntSettingUnit
import com.movtery.zalithlauncher.setting.unit.StringSettingUnit
import com.movtery.zalithlauncher.ui.components.SimpleIntSliderLayout
import com.movtery.zalithlauncher.ui.components.SimpleListLayout
import com.movtery.zalithlauncher.ui.components.SwitchLayout
import com.movtery.zalithlauncher.ui.components.TextInputLayout
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import kotlin.enums.EnumEntries

@DslMarker
annotation class SettingsLayoutDsl

@SettingsLayoutDsl
class SettingsLayoutScope {
    @Composable
    fun SwitchSettingsLayout(
        modifier: Modifier = Modifier,
        unit: BooleanSettingUnit,
        title: String,
        summary: String? = null,
        onCheckedChange: (Boolean) -> Unit = {},
        trailingIcon: @Composable (() -> Unit)? = null
    ) {
        var checked by rememberSaveable { mutableStateOf(unit.getValue()) }

        fun change(value: Boolean) {
            checked = value
            unit.put(checked).save()
            onCheckedChange(checked)
        }

        SwitchLayout(
            checked = checked,
            onCheckedChange = { value ->
                change(value)
            },
            modifier = modifier,
            title = title,
            summary = summary,
            trailingIcon = trailingIcon
        )
    }

    @Composable
    fun SliderSettingsLayout(
        modifier: Modifier = Modifier,
        unit: IntSettingUnit,
        title: String,
        summary: String? = null,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int = 0,
        suffix: String? = null,
        onValueChange: (Int) -> Unit = {},
        enabled: Boolean = true,
        fineTuningControl: Boolean = false
    ) {
        var value by rememberSaveable { mutableIntStateOf(unit.getValue()) }

        SimpleIntSliderLayout(
            modifier = modifier,
            value = value,
            title = title,
            summary = summary,
            valueRange = valueRange,
            steps = steps,
            suffix = suffix,
            onValueChange = {
                value = it
                onValueChange(value)
            },
            onValueChangeFinished = { unit.put(value).save() },
            enabled = enabled,
            fineTuningControl = fineTuningControl
        )
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun <E: Enum<E>> EnumSettingsLayout(
        modifier: Modifier = Modifier,
        unit: StringSettingUnit,
        entries: EnumEntries<E>,
        title: String,
        summary: String? = null,
        getRadioText: @Composable (E) -> String,
        getRadioEnable: (E) -> Boolean,
        onRadioClick: (E) -> Unit = {},
        onValueChange: (E) -> Unit = {}
    ) {
        var value by rememberSaveable { mutableStateOf(unit.getValue()) }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(all = 8.dp)
                .padding(bottom = 4.dp)
        ) {
            TitleAndSummary(title, summary)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = getAnimateTween()),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                entries.forEach { enum ->
                    Row {
                        val radioText = getRadioText(enum)
                        RadioButton(
                            enabled = getRadioEnable(enum),
                            selected = value == enum.name,
                            onClick = {
                                onRadioClick(enum)
                                if (value == enum.name) return@RadioButton
                                value = enum.name
                                unit.put(value).save()
                                onValueChange(enum)
                            }
                        )
                        Text(
                            text = radioText,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .alpha(alpha = if (getRadioEnable(enum)) 1f else 0.5f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun <E> ListSettingsLayout(
        modifier: Modifier = Modifier,
        unit: StringSettingUnit,
        items: List<E>,
        title: String,
        summary: String? = null,
        getItemText: @Composable (E) -> String,
        getItemId: (E) -> String,
        getItemSummary: (@Composable (E) -> Unit)? = null,
        enabled: Boolean = true,
        itemListPadding: PaddingValues = PaddingValues(bottom = 4.dp),
        onValueChange: (E) -> Unit = {}
    ) {
        SimpleListLayout(
            modifier = modifier,
            items = items,
            currentId = unit.getValue(),
            defaultId = unit.defaultValue,
            title = title,
            summary = summary,
            getItemText = getItemText,
            getItemId = getItemId,
            getItemSummary = getItemSummary,
            enabled = enabled,
            itemListPadding = itemListPadding,
            onValueChange = { item ->
                unit.put(getItemId(item)).save()
                onValueChange(item)
            }
        )
    }

    @Composable
    fun TextInputSettingsLayout(
        modifier: Modifier = Modifier,
        unit: StringSettingUnit,
        title: String,
        summary: String? = null,
        label: String? = null,
        onValueChange: (String) -> Unit = {},
        singleLine: Boolean = true
    ) {
        TextInputLayout(
            modifier = modifier,
            currentValue = unit.getValue(),
            title = title,
            summary = summary,
            onValueChange = { value ->
                unit.put(value).save()
                onValueChange(value)
            },
            label = {
                Text(text = label ?: stringResource(R.string.settings_label_ignore_if_blank))
            },
            singleLine = singleLine
        )
    }
}
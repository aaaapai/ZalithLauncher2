package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.utils.animation.getAnimateTween

@Composable
fun ScalingLabel(
    modifier: Modifier = Modifier,
    text: String,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }
    Surface(
        modifier = modifier.graphicsLayer(scaleY = scale.value, scaleX = scale.value),
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation
    ) {
        Text(
            modifier = Modifier.padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp)),
            text = text
        )
    }
}

@Composable
fun ScalingLabel(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp
) {
    val scale = remember { Animatable(initialValue = 0.95f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = getAnimateTween())
    }
    Surface(
        modifier = modifier.graphicsLayer(scaleY = scale.value, scaleX = scale.value),
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        onClick = onClick
    ) {
        Text(
            modifier = Modifier.padding(PaddingValues(horizontal = 12.dp, vertical = 8.dp)),
            text = text
        )
    }
}

@Composable
fun LittleTextLabel(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = MaterialTheme.colorScheme.tertiary,
    contentColor: Color = MaterialTheme.colorScheme.onTertiary,
    shape: Shape = MaterialTheme.shapes.large,
    textStyle: TextStyle = MaterialTheme.typography.labelMedium
) {
    Surface(
        modifier = modifier,
        color = color,
        contentColor = contentColor,
        shape = shape
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            text = text,
            style = textStyle
        )
    }
}

@Composable
fun <E> SimpleListLayout(
    modifier: Modifier = Modifier,
    items: List<E>,
    currentId: String,
    defaultId: String,
    title: String,
    summary: String? = null,
    getItemText: @Composable (E) -> String,
    getItemId: (E) -> String,
    getItemSummary: (@Composable (E) -> Unit)? = null,
    enabled: Boolean = true,
    autoCollapse: Boolean = true,
    itemListPadding: PaddingValues = PaddingValues(bottom = 4.dp),
    onValueChange: (E) -> Unit = {},
    selectableAreaShape: Shape = RoundedCornerShape(22.0.dp)
) {
    require(items.isNotEmpty()) { "Items list cannot be empty" }

    var selectedItem by remember {
        mutableStateOf(
            items.firstOrNull { getItemId(it) == currentId }
                ?: items.firstOrNull { getItemId(it) == defaultId }
                ?: items.first()
        )
    }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) expanded = false
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha = if (enabled) 1f else 0.5f)
            .padding(bottom = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = selectableAreaShape)
                    .clickable(enabled = enabled) { expanded = !expanded }
                    .padding(all = 8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TitleAndSummary(
                        title = title,
                        summary = summary
                    )
                    Text(
                        modifier = Modifier.alpha(0.7f),
                        text = stringResource(R.string.settings_element_selected, getItemText(selectedItem)),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) -180f else 0f,
                    animationSpec = getAnimateTween()
                )
                IconButton(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(34.dp)
                        .rotate(rotation),
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = stringResource(if (expanded) R.string.generic_expand else R.string.generic_collapse)
                    )
                }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(animationSpec = getAnimateTween()),
                    exit = shrinkVertically(animationSpec = getAnimateTween()) + fadeOut(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(itemListPadding)
                    ) {
                        items.forEach { item ->
                            SimpleListItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 3.dp),
                                selected = getItemId(selectedItem) == getItemId(item),
                                itemName = getItemText(item),
                                summary = getItemSummary?.let {
                                    { it.invoke(item) }
                                },
                                onClick = {
                                    if (expanded && getItemId(selectedItem) != getItemId(item)) {
                                        selectedItem = item
                                        onValueChange(item)
                                        if (autoCollapse) expanded = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleListItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    itemName: String,
    summary: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = itemName,
                style = MaterialTheme.typography.labelMedium
            )
            summary?.invoke()
        }
    }
}

data class IDItem(val id: String, val title: String)

@Composable
fun SimpleIDListLayout(
    modifier: Modifier = Modifier,
    items: List<IDItem>,
    currentId: String,
    defaultId: String,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    itemListPadding: PaddingValues = PaddingValues(bottom = 4.dp),
    onValueChange: (IDItem) -> Unit = {}
) {
    SimpleListLayout(
        modifier = modifier,
        items = items,
        currentId = currentId,
        defaultId = defaultId,
        title = title,
        summary = summary,
        getItemText = { it.title },
        getItemId = { it.id },
        enabled = enabled,
        itemListPadding = itemListPadding,
        onValueChange = onValueChange
    )
}

@Composable
fun TextInputLayout(
    modifier: Modifier = Modifier,
    currentValue: String = "",
    title: String,
    summary: String? = null,
    onValueChange: (String) -> Unit = {},
    label: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true
) {
    var value by remember { mutableStateOf(currentValue) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp)
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TitleAndSummary(title = title, summary = summary)
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            textStyle = MaterialTheme.typography.labelMedium,
            onValueChange = {
                value = it
                onValueChange(value)
            },
            label = label,
            supportingText = supportingText,
            singleLine = singleLine,
            shape = MaterialTheme.shapes.large
        )
    }
}

@Composable
fun SimpleIntSliderLayout(
    modifier: Modifier = Modifier,
    value: Int,
    title: String,
    summary: String? = null,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    suffix: String? = null,
    onValueChange: (Int) -> Unit = {},
    onValueChangeFinished: () -> Unit = {},
    enabled: Boolean = true,
    fineTuningControl: Boolean = false,
    appendContent: @Composable () -> Unit = {}
) {
    var showValueEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp)
            .padding(bottom = 4.dp)
    ) {
        Column(
            modifier = Modifier.alpha(alpha = if (enabled) 1f else 0.5f)
        ) {
            TitleAndSummary(
                title = title,
                summary = summary
            )
        }
        SimpleTextSlider(
            modifier = Modifier.fillMaxWidth(),
            value = value.toFloat(),
            enabled = enabled,
            onValueChange = { onValueChange(it.toInt()) },
            onValueChangeFinished = { onValueChangeFinished() },
            onTextClick = { showValueEditDialog = true },
            toInt = true,
            valueRange = valueRange,
            steps = steps,
            suffix = suffix,
            fineTuningControl = fineTuningControl,
            fineTuningStep = 1f,
            appendContent = appendContent
        )
    }

    if (showValueEditDialog) {
        val maxLength = listOf(
            valueRange.start.toInt().toString().length,
            valueRange.endInclusive.toInt().toString().length
        ).maxOrNull() ?: 5

        var inputValue by remember { mutableStateOf(value.toString()) }
        var errorText by remember { mutableStateOf("") }
        val numberFormatError = stringResource(R.string.generic_input_failed_to_number)
        val numberTooSmallError = stringResource(R.string.generic_input_too_small, valueRange.start.toInt())
        val numberTooLargeError = stringResource(R.string.generic_input_too_large, valueRange.endInclusive.toInt())

        SimpleEditDialog(
            title = title,
            value = inputValue,
            onValueChange = { newInput ->
                val filteredInput = newInput.take(maxLength)
                inputValue = filteredInput

                val result = filteredInput.toIntOrNull()
                errorText = when {
                    result == null -> numberFormatError
                    result < valueRange.start -> numberTooSmallError
                    result > valueRange.endInclusive -> numberTooLargeError
                    else -> ""
                }
            },
            isError = errorText.isNotEmpty(),
            supportingText = {
                if (errorText.isNotEmpty()) Text(text = errorText)
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
            onConfirm = {
                if (errorText.isEmpty()) {
                    val newValue = inputValue.toIntOrNull() ?: value
                    onValueChange(newValue)
                    onValueChangeFinished()
                    showValueEditDialog = false
                }
            },
            onDismissRequest = { showValueEditDialog = false }
        )
    }
}

@Composable
fun SwitchLayout(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    shape: Shape = RoundedCornerShape(22.0.dp),
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = shape)
            .clickable { onCheckedChange(!checked) }
            .padding(all = 8.dp)
            .padding(bottom = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            TitleAndSummary(
                title = title,
                summary = summary
            )
        }

        trailingIcon?.invoke()

        Switch(
            modifier = Modifier.align(Alignment.CenterVertically),
            checked = checked,
            onCheckedChange = { value -> onCheckedChange(value) }
        )
    }
}

@Composable
fun TitleAndSummary(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall
        )
        summary?.let { text ->
            Text(
                modifier = Modifier.alpha(0.7f),
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun FocusableBox(
    modifier: Modifier = Modifier,
    requestKey: Any? = null
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .focusable(enabled = true)
            .focusRequester(focusRequester)
    )

    LaunchedEffect(requestKey) {
        focusRequester.requestFocus()
    }
}
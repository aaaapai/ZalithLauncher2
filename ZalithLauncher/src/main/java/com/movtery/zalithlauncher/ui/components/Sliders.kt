package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowLeft
import androidx.compose.material.icons.automirrored.rounded.ArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.utils.math.addBigDecimal
import com.movtery.zalithlauncher.utils.math.subtractBigDecimal
import java.text.DecimalFormat

@Composable
fun SimpleTextSlider(
    modifier: Modifier = Modifier,
    value: Float,
    enabled: Boolean = true,
    onValueChange: (Float) -> Unit,
    toInt: Boolean = false,
    suffix: String? = null,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    onTextClick: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    fineTuningControl: Boolean = false,
    fineTuningStep: Float = 0.5f,
    appendContent: @Composable () -> Unit = {}
) {
    val formatter = DecimalFormat("#0.00")
    fun getTextString(value: Float) = "${if (toInt) {
        value.toInt()
    } else {
        formatter.format(value)
    }}${if (suffix == null) "" else " $suffix"}"

    fun changeValue(newValue: Float, finished: Boolean) {
        onValueChange(newValue)
        if (finished) onValueChangeFinished?.invoke()
    }

    LaunchedEffect(Unit) {
        //检查值是否被刻意的修改为超出范围
        if (value !in valueRange) {
            val newValue = value.coerceIn(valueRange)
            //调回范围内
            changeValue(newValue, true)
        }
    }

    Row(
        modifier = modifier
    ) {
        Slider(
            value = value,
            enabled = enabled,
            onValueChange = { changeValue(it, false) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f)
        )
        Surface(
            modifier = Modifier
                .alpha(alpha = if (enabled) 1f else 0.5f)
                .padding(start = 12.dp)
                .align(Alignment.CenterVertically),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.padding(PaddingValues(horizontal = 8.dp, vertical = 4.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.then(
                        if (onTextClick != null) {
                            Modifier.clickable(enabled = enabled, onClick = onTextClick)
                        } else Modifier
                    ),
                    text = getTextString(value),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                if (fineTuningControl) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        enabled = enabled,
                        modifier = Modifier.size(26.dp),
                        onClick = {
                            val newValue = value.subtractBigDecimal(fineTuningStep)
                            if (newValue <= valueRange.start) {
                                changeValue(valueRange.start, true)
                            } else {
                                changeValue(newValue, true)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowLeft,
                            contentDescription = null
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        enabled = enabled,
                        modifier = Modifier.size(26.dp),
                        onClick = {
                            val newValue = value.addBigDecimal(fineTuningStep)
                            if (newValue >= valueRange.endInclusive) {
                                changeValue(valueRange.endInclusive, true)
                            } else {
                                changeValue(newValue, true)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowRight,
                            contentDescription = null
                        )
                    }
                }
            }
        }
        appendContent()
    }
}
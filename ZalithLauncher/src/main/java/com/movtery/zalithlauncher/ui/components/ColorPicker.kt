package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import com.github.skydoves.colorpicker.compose.AlphaSlider
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.movtery.zalithlauncher.R

/**
 * 一个简易的颜色选择器
 * @param initialColor 初始颜色（作为对比进行展示，以及用户取消之后进行还原）
 * @param realTimeUpdate 实时颜色变更回调
 * @param onColorChanged 颜色变更
 * @param showAlpha 是否使用透明度调节器
 * @param showBrightness 是否使用明度调节器
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color = Color.White,
    realTimeUpdate: Boolean = true,
    onColorChanged: (Color) -> Unit = {},
    onDismissRequest: () -> Unit,
    onConfirm: (Color) -> Unit,
    showAlpha: Boolean = true,
    showBrightness: Boolean = true
) {
    val colorController = rememberColorPickerController()

    LaunchedEffect(Unit) {
        //设置初始颜色
        colorController.selectByColor(initialColor, true)
    }
    val selectedColor = colorController.selectedColor.value
    val selectedHex = selectedColor.toHex().also {
        if (realTimeUpdate) {
            //实时更新
            onColorChanged(selectedColor)
        }
    }

    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier.fillMaxHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(all = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.theme_color_picker_title),
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HsvColorPicker(
                        modifier = Modifier.weight(1f),
                        controller = colorController
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(state = rememberScrollState())
                    ) {
                        if (showAlpha) {
                            AlphaSlider(
                                modifier = Modifier
                                    .height(30.dp)
                                    .fillMaxWidth(),
                                controller = colorController
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (showBrightness) {
                            BrightnessSlider(
                                modifier = Modifier
                                    .height(30.dp)
                                    .fillMaxWidth(),
                                controller = colorController
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        //颜色预览
                        ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
                            val (initialHex, initialBox, arrow, currentHex, currentBox) = createRefs()

                            //初始颜色
                            Text(
                                modifier = Modifier.constrainAs(initialHex) {
                                    start.linkTo(parent.start)
                                    top.linkTo(parent.top)
                                },
                                text = initialColor.toHex(),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Box(
                                modifier = Modifier
                                    .constrainAs(initialBox) {
                                        start.linkTo(parent.start)
                                        top.linkTo(anchor = initialHex.bottom, margin = 4.dp)
                                    }
                                    .size(50.dp)
                                    .background(color = initialColor, shape = MaterialTheme.shapes.medium)
                            )

                            Icon(
                                modifier = Modifier
                                    .constrainAs(arrow) {
                                        top.linkTo(initialBox.top)
                                        bottom.linkTo(initialBox.bottom)
                                        start.linkTo(initialBox.end)
                                        end.linkTo(currentBox.start)
                                    },
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null
                            )

                            //当前颜色
                            Text(
                                modifier = Modifier.constrainAs(currentHex) {
                                    end.linkTo(parent.end)
                                    top.linkTo(parent.top)
                                },
                                text = selectedHex,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Box(
                                modifier = Modifier
                                    .constrainAs(currentBox) {
                                        end.linkTo(parent.end)
                                        top.linkTo(anchor = currentHex.bottom, margin = 4.dp)
                                    }
                                    .size(50.dp)
                                    .background(color = selectedColor, shape = MaterialTheme.shapes.medium)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onColorChanged(initialColor)
                            onDismissRequest()
                        }
                    ) {
                        Text(text = stringResource(R.string.generic_cancel))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onConfirm(selectedColor)
                        }
                    ) {
                        Text(text = stringResource(R.string.generic_confirm))
                    }
                }
            }
        }
    }
}

/**
 * 将颜色转换为Hex字符串
 */
fun Color.toHex(): String {
    val alpha = (alpha * 255).toInt().toString(16).padStart(2, '0')
    val red = (red * 255).toInt().toString(16).padStart(2, '0')
    val green = (green * 255).toInt().toString(16).padStart(2, '0')
    val blue = (blue * 255).toInt().toString(16).padStart(2, '0')
    return "$alpha$red$green$blue".uppercase()
}
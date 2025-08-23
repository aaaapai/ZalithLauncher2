package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import com.movtery.zalithlauncher.utils.animation.getAnimateTweenJellyBounce
import com.movtery.zalithlauncher.utils.animation.swapAnimateDpAsState

/**
 * 菜单状态
 */
enum class MenuState {
    /**
     * 初始化 (刚加载时)
     */
    NONE {
        override fun next() = SHOW
    },

    /**
     * 展示中
     */
    SHOW {
        override fun next() = HIDE
    },

    /**
     * 隐藏中
     */
    HIDE {
        override fun next() = SHOW
    };

    abstract fun next(): MenuState
}

@Composable
fun MenuSubscreen(
    state: MenuState,
    closeScreen: () -> Unit,
    shape: Shape = RoundedCornerShape(21.0.dp),
    backgroundColor: Color = Color.Black.copy(alpha = 0.25f),
    backgroundAnimDuration: Int = 150,
    content: @Composable ColumnScope.() -> Unit
) {
    val visible = state == MenuState.SHOW
    val animationProgress = remember { Animatable(0f) }
    var shouldRender by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            shouldRender = true
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(backgroundAnimDuration)
            )
        } else {
            animationProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(backgroundAnimDuration)
            )
            shouldRender = false
        }
    }

    val bgAlpha by remember {
        derivedStateOf { animationProgress.value }
    }
    val menuOffset by swapAnimateDpAsState(
        targetValue = 40.dp,
        swapIn = visible,
        isHorizontal = true,
        animationSpec = getAnimateTweenJellyBounce()
    )

    if (shouldRender) {
        Box(modifier = Modifier.fillMaxSize()) {
            //背景阴影层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(bgAlpha)
                    .background(color = backgroundColor)
                    .clickable(
                        indication = null, //禁用水波纹点击效果
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = closeScreen
                    )
            )

            //Menu
            if (animationProgress.value > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxWidth(fraction = 1f / 3f)
                        .fillMaxHeight()
                        .padding(top = 12.dp, end = 12.dp, bottom = 12.dp)
                        .offset {
                            IntOffset(x = menuOffset.roundToPx(), y = 0)
                        }
                ) {
                    Card(
                        shape = shape,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(animationProgress.value),
                        content = content
                    )
                }
            }
        }
    }
}

@Composable
fun MenuTextButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp,
    onClick: () -> Unit = {}
) {
    MenuButtonLayout(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        enabled = enabled,
        onClick = onClick,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MarqueeText(
            modifier = Modifier
                .padding(all = 16.dp)
                .alpha(if (enabled) 1f else 0.5f),
            text = text,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun MenuSwitchButton(
    modifier: Modifier = Modifier,
    text: String,
    switch: Boolean,
    onSwitch: (Boolean) -> Unit,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp
) {
    MenuButtonLayout(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        enabled = enabled,
        onClick = { onSwitch(!switch) }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MarqueeText(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .weight(1f)
                    .alpha(if (enabled) 1f else 0.5f),
                text = text,
                style = MaterialTheme.typography.titleSmall
            )
            Switch(
                checked = switch,
                onCheckedChange = onSwitch,
                enabled = enabled
            )
        }
    }
}

@Composable
fun <E> MenuListLayout(
    modifier: Modifier = Modifier,
    title: String,
    items: List<E>,
    currentItem: E,
    onItemChange: (E) -> Unit,
    getItemText: @Composable (E) -> String,
    selectedItemLayout: @Composable (ColumnScope.(E) -> Unit) = { item ->
        LittleTextLabel(
            text = getItemText(item)
        )
    },
    enabled: Boolean = true,
    maxListHeight: Dp = 200.dp,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp
) {
    var expanded by remember { mutableStateOf(false) }

    MenuButtonLayout(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation,
        enabled = enabled,
        onClick = {}
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MenuListHeader(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else 0.5f),
                items = items,
                title = title,
                selectedItemLayout = {
                    selectedItemLayout(currentItem)
                },
                expanded = expanded,
                enable = enabled,
                onClick = {
                    expanded = !expanded
                }
            )

            if (enabled && items.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(animationSpec = getAnimateTween()),
                        exit = shrinkVertically(animationSpec = getAnimateTween()) + fadeOut(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = maxListHeight)
                                .padding(vertical = 4.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(items) { item ->
                                MenuListItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(all = 4.dp),
                                    text = getItemText(item),
                                    selected = currentItem == item,
                                    onClick = {
                                        if (expanded && currentItem != item) {
                                            onItemChange(item)
                                            expanded = false
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
}

@Composable
private fun <E> MenuListHeader(
    modifier: Modifier = Modifier,
    items: List<E>,
    title: String,
    selectedItemLayout: @Composable ColumnScope.() -> Unit,
    expanded: Boolean,
    enable: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick, enabled = enable)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MarqueeText(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            selectedItemLayout()
        }

        if (!items.isEmpty()) {
            Row(
                modifier = Modifier.padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) -180f else 0f,
                    animationSpec = getAnimateTween()
                )
                Icon(
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotation),
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = stringResource(if (expanded) R.string.generic_expand else R.string.generic_collapse)
                )
            }
        }
    }
}

@Composable
private fun MenuListItem(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(shape = MaterialTheme.shapes.medium)
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
            MarqueeText(
                text = text,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSliderLayout(
    modifier: Modifier = Modifier,
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    onValueChangeFinished: (Int) -> Unit = {},
    suffix: String? = null,
    colors: SliderColors = SliderDefaults.colors(),
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    MenuButtonLayout(
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = color,
        contentColor = contentColor,
        shadowElevation = shadowElevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .alpha(if (enabled) 1f else 0.5f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MarqueeText(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "$value${suffix ?: ""}",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            /** Slider顶部需要裁切的像素 */
            val sliderTopCut = with(LocalDensity.current) { 8.dp.toPx().toInt() }
            /** Slider底部需要裁切的像素 */
            val sliderBottomCut = with(LocalDensity.current) { 6.dp.toPx().toInt() }
            Layout(
                content = {
                    Slider(
                        modifier = Modifier.fillMaxWidth(),
                        value = value.toFloat(),
                        onValueChange = { onValueChange(it.toInt()) },
                        valueRange = valueRange,
                        enabled = enabled,
                        onValueChangeFinished = { onValueChangeFinished(value) },
                        interactionSource = interactionSource,
                        colors = colors,
                        thumb = {
                            SliderDefaults.Thumb(
                                interactionSource = interactionSource,
                                colors = colors,
                                enabled = enabled,
                                thumbSize = DpSize(4.0.dp, 18.5.dp)
                            )
                        }
                    )
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                val newHeight = (placeable.height - sliderTopCut - sliderBottomCut).coerceAtLeast(0)
                layout(placeable.width, newHeight) {
                    placeable.place(0, -sliderTopCut)
                }
            }
        }
    }
}

@Composable
fun MenuButtonLayout(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = itemLayoutColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    shadowElevation: Dp = 1.dp,
    onClick: () -> Unit = {},
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
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
        enabled = enabled,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = content
        )
    }
}
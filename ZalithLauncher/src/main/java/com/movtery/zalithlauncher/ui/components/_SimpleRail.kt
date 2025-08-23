package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun TextRailItem(
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    selected: Boolean,
    shape: Shape = MaterialTheme.shapes.large,
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer.desaturate(0.5f)
) {
    val animationProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "SelectionAnimation"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
    ) {
        //背景扩散动画
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .alpha(animationProgress)
        ) {
            val maxWidth = size.width
            val minWidth = 0f
            val currentWidth = minWidth + (maxWidth - minWidth) * animationProgress

            val left = (maxWidth - currentWidth) / 2

            //绘制胶囊形状背景
            drawRoundRect(
                color = backgroundColor,
                topLeft = Offset(left, 0f),
                size = Size(currentWidth, size.height),
                cornerRadius = CornerRadius(size.height / 2, size.height / 2)
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            text()
        }
    }
}
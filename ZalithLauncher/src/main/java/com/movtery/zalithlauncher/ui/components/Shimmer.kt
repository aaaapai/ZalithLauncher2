package com.movtery.zalithlauncher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

@Composable
fun Modifier.infiniteShimmer(
    initialValue: Float = 0.3f,
    targetValue: Float = 0.6f
): Modifier {
    val infiniteTransition = rememberInfiniteTransition()

    //循环动画
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = initialValue,
        targetValue = targetValue,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    return this.then(
        Modifier.alpha(animatedAlpha)
    )
}

/**
 * 无限循环闪烁Box，可用于制作加载时骨架
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    color: Color = Color.Gray,
    shape: Shape = RectangleShape
) {


    Box(
        modifier = modifier
            .infiniteShimmer()
            .background(
                color = color,
                shape = shape
            )
    )
}
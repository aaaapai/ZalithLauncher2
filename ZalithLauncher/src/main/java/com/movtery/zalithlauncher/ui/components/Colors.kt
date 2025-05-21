package com.movtery.zalithlauncher.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * 降低颜色的饱和度
 */
fun Color.desaturate(factor: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[1] *= factor.coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun itemLayoutColor(): Color {
    return if (isSystemInDarkTheme()) {
        //暗色模式下如果用 surfaceColorAtElevation(1.dp) 就会太黑太突出
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        //浅色模式下用这个颜色刚刚好
        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    }
}

/**
 * 在 secondaryContainer 背景上使用的 NavigationDrawerItem 颜色
 */
@Composable
fun secondaryContainerDrawerItemColors(): NavigationDrawerItemColors {
    val colorScheme = MaterialTheme.colorScheme
    return NavigationDrawerItemDefaults.colors(
        selectedContainerColor = colorScheme.secondaryContainer.desaturate(0.5f),
        unselectedContainerColor = Color.Transparent,
        selectedIconColor = colorScheme.onSecondaryContainer,
        unselectedIconColor = colorScheme.onSurface,
        selectedTextColor = colorScheme.onSecondaryContainer,
        unselectedTextColor = colorScheme.onSurface,
    )
}
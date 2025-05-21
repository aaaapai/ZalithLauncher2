package com.movtery.zalithlauncher.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.Scheme
import com.movtery.zalithlauncher.state.ColorThemeState
import com.movtery.zalithlauncher.state.CustomColorThemeState
import com.movtery.zalithlauncher.state.LocalColorThemeState
import com.movtery.zalithlauncher.state.LocalCustomColorThemeState
import com.movtery.zalithlauncher.utils.animation.getAnimateTween

private val embermireLight = lightColorScheme(
    primary = primaryLight.embermire,
    onPrimary = onPrimaryLight.embermire,
    primaryContainer = primaryContainerLight.embermire,
    onPrimaryContainer = onPrimaryContainerLight.embermire,
    secondary = secondaryLight.embermire,
    onSecondary = onSecondaryLight.embermire,
    secondaryContainer = secondaryContainerLight.embermire,
    onSecondaryContainer = onSecondaryContainerLight.embermire,
    tertiary = tertiaryLight.embermire,
    onTertiary = onTertiaryLight.embermire,
    tertiaryContainer = tertiaryContainerLight.embermire,
    onTertiaryContainer = onTertiaryContainerLight.embermire,
    error = errorLight.embermire,
    onError = onErrorLight.embermire,
    errorContainer = errorContainerLight.embermire,
    onErrorContainer = onErrorContainerLight.embermire,
    background = backgroundLight.embermire,
    onBackground = onBackgroundLight.embermire,
    surface = surfaceLight.embermire,
    onSurface = onSurfaceLight.embermire,
    surfaceVariant = surfaceVariantLight.embermire,
    onSurfaceVariant = onSurfaceVariantLight.embermire,
    outline = outlineLight.embermire,
    outlineVariant = outlineVariantLight.embermire,
    scrim = scrimLight.embermire,
    inverseSurface = inverseSurfaceLight.embermire,
    inverseOnSurface = inverseOnSurfaceLight.embermire,
    inversePrimary = inversePrimaryLight.embermire,
    surfaceDim = surfaceDimLight.embermire,
    surfaceBright = surfaceBrightLight.embermire,
    surfaceContainerLowest = surfaceContainerLowestLight.embermire,
    surfaceContainerLow = surfaceContainerLowLight.embermire,
    surfaceContainer = surfaceContainerLight.embermire,
    surfaceContainerHigh = surfaceContainerHighLight.embermire,
    surfaceContainerHighest = surfaceContainerHighestLight.embermire,
)

private val embermireDark = darkColorScheme(
    primary = primaryDark.embermire,
    onPrimary = onPrimaryDark.embermire,
    primaryContainer = primaryContainerDark.embermire,
    onPrimaryContainer = onPrimaryContainerDark.embermire,
    secondary = secondaryDark.embermire,
    onSecondary = onSecondaryDark.embermire,
    secondaryContainer = secondaryContainerDark.embermire,
    onSecondaryContainer = onSecondaryContainerDark.embermire,
    tertiary = tertiaryDark.embermire,
    onTertiary = onTertiaryDark.embermire,
    tertiaryContainer = tertiaryContainerDark.embermire,
    onTertiaryContainer = onTertiaryContainerDark.embermire,
    error = errorDark.embermire,
    onError = onErrorDark.embermire,
    errorContainer = errorContainerDark.embermire,
    onErrorContainer = onErrorContainerDark.embermire,
    background = backgroundDark.embermire,
    onBackground = onBackgroundDark.embermire,
    surface = surfaceDark.embermire,
    onSurface = onSurfaceDark.embermire,
    surfaceVariant = surfaceVariantDark.embermire,
    onSurfaceVariant = onSurfaceVariantDark.embermire,
    outline = outlineDark.embermire,
    outlineVariant = outlineVariantDark.embermire,
    scrim = scrimDark.embermire,
    inverseSurface = inverseSurfaceDark.embermire,
    inverseOnSurface = inverseOnSurfaceDark.embermire,
    inversePrimary = inversePrimaryDark.embermire,
    surfaceDim = surfaceDimDark.embermire,
    surfaceBright = surfaceBrightDark.embermire,
    surfaceContainerLowest = surfaceContainerLowestDark.embermire,
    surfaceContainerLow = surfaceContainerLowDark.embermire,
    surfaceContainer = surfaceContainerDark.embermire,
    surfaceContainerHigh = surfaceContainerHighDark.embermire,
    surfaceContainerHighest = surfaceContainerHighestDark.embermire,
)

private val velvetRoseLight = lightColorScheme(
    primary = primaryLight.velvetRose,
    onPrimary = onPrimaryLight.velvetRose,
    primaryContainer = primaryContainerLight.velvetRose,
    onPrimaryContainer = onPrimaryContainerLight.velvetRose,
    secondary = secondaryLight.velvetRose,
    onSecondary = onSecondaryLight.velvetRose,
    secondaryContainer = secondaryContainerLight.velvetRose,
    onSecondaryContainer = onSecondaryContainerLight.velvetRose,
    tertiary = tertiaryLight.velvetRose,
    onTertiary = onTertiaryLight.velvetRose,
    tertiaryContainer = tertiaryContainerLight.velvetRose,
    onTertiaryContainer = onTertiaryContainerLight.velvetRose,
    error = errorLight.velvetRose,
    onError = onErrorLight.velvetRose,
    errorContainer = errorContainerLight.velvetRose,
    onErrorContainer = onErrorContainerLight.velvetRose,
    background = backgroundLight.velvetRose,
    onBackground = onBackgroundLight.velvetRose,
    surface = surfaceLight.velvetRose,
    onSurface = onSurfaceLight.velvetRose,
    surfaceVariant = surfaceVariantLight.velvetRose,
    onSurfaceVariant = onSurfaceVariantLight.velvetRose,
    outline = outlineLight.velvetRose,
    outlineVariant = outlineVariantLight.velvetRose,
    scrim = scrimLight.velvetRose,
    inverseSurface = inverseSurfaceLight.velvetRose,
    inverseOnSurface = inverseOnSurfaceLight.velvetRose,
    inversePrimary = inversePrimaryLight.velvetRose,
    surfaceDim = surfaceDimLight.velvetRose,
    surfaceBright = surfaceBrightLight.velvetRose,
    surfaceContainerLowest = surfaceContainerLowestLight.velvetRose,
    surfaceContainerLow = surfaceContainerLowLight.velvetRose,
    surfaceContainer = surfaceContainerLight.velvetRose,
    surfaceContainerHigh = surfaceContainerHighLight.velvetRose,
    surfaceContainerHighest = surfaceContainerHighestLight.velvetRose,
)

private val velvetRoseDark = darkColorScheme(
    primary = primaryDark.velvetRose,
    onPrimary = onPrimaryDark.velvetRose,
    primaryContainer = primaryContainerDark.velvetRose,
    onPrimaryContainer = onPrimaryContainerDark.velvetRose,
    secondary = secondaryDark.velvetRose,
    onSecondary = onSecondaryDark.velvetRose,
    secondaryContainer = secondaryContainerDark.velvetRose,
    onSecondaryContainer = onSecondaryContainerDark.velvetRose,
    tertiary = tertiaryDark.velvetRose,
    onTertiary = onTertiaryDark.velvetRose,
    tertiaryContainer = tertiaryContainerDark.velvetRose,
    onTertiaryContainer = onTertiaryContainerDark.velvetRose,
    error = errorDark.velvetRose,
    onError = onErrorDark.velvetRose,
    errorContainer = errorContainerDark.velvetRose,
    onErrorContainer = onErrorContainerDark.velvetRose,
    background = backgroundDark.velvetRose,
    onBackground = onBackgroundDark.velvetRose,
    surface = surfaceDark.velvetRose,
    onSurface = onSurfaceDark.velvetRose,
    surfaceVariant = surfaceVariantDark.velvetRose,
    onSurfaceVariant = onSurfaceVariantDark.velvetRose,
    outline = outlineDark.velvetRose,
    outlineVariant = outlineVariantDark.velvetRose,
    scrim = scrimDark.velvetRose,
    inverseSurface = inverseSurfaceDark.velvetRose,
    inverseOnSurface = inverseOnSurfaceDark.velvetRose,
    inversePrimary = inversePrimaryDark.velvetRose,
    surfaceDim = surfaceDimDark.velvetRose,
    surfaceBright = surfaceBrightDark.velvetRose,
    surfaceContainerLowest = surfaceContainerLowestDark.velvetRose,
    surfaceContainerLow = surfaceContainerLowDark.velvetRose,
    surfaceContainer = surfaceContainerDark.velvetRose,
    surfaceContainerHigh = surfaceContainerHighDark.velvetRose,
    surfaceContainerHighest = surfaceContainerHighestDark.velvetRose,
)

private val mistwaveLight = lightColorScheme(
    primary = primaryLight.mistwave,
    onPrimary = onPrimaryLight.mistwave,
    primaryContainer = primaryContainerLight.mistwave,
    onPrimaryContainer = onPrimaryContainerLight.mistwave,
    secondary = secondaryLight.mistwave,
    onSecondary = onSecondaryLight.mistwave,
    secondaryContainer = secondaryContainerLight.mistwave,
    onSecondaryContainer = onSecondaryContainerLight.mistwave,
    tertiary = tertiaryLight.mistwave,
    onTertiary = onTertiaryLight.mistwave,
    tertiaryContainer = tertiaryContainerLight.mistwave,
    onTertiaryContainer = onTertiaryContainerLight.mistwave,
    error = errorLight.mistwave,
    onError = onErrorLight.mistwave,
    errorContainer = errorContainerLight.mistwave,
    onErrorContainer = onErrorContainerLight.mistwave,
    background = backgroundLight.mistwave,
    onBackground = onBackgroundLight.mistwave,
    surface = surfaceLight.mistwave,
    onSurface = onSurfaceLight.mistwave,
    surfaceVariant = surfaceVariantLight.mistwave,
    onSurfaceVariant = onSurfaceVariantLight.mistwave,
    outline = outlineLight.mistwave,
    outlineVariant = outlineVariantLight.mistwave,
    scrim = scrimLight.mistwave,
    inverseSurface = inverseSurfaceLight.mistwave,
    inverseOnSurface = inverseOnSurfaceLight.mistwave,
    inversePrimary = inversePrimaryLight.mistwave,
    surfaceDim = surfaceDimLight.mistwave,
    surfaceBright = surfaceBrightLight.mistwave,
    surfaceContainerLowest = surfaceContainerLowestLight.mistwave,
    surfaceContainerLow = surfaceContainerLowLight.mistwave,
    surfaceContainer = surfaceContainerLight.mistwave,
    surfaceContainerHigh = surfaceContainerHighLight.mistwave,
    surfaceContainerHighest = surfaceContainerHighestLight.mistwave,
)

private val mistwaveDark = darkColorScheme(
    primary = primaryDark.mistwave,
    onPrimary = onPrimaryDark.mistwave,
    primaryContainer = primaryContainerDark.mistwave,
    onPrimaryContainer = onPrimaryContainerDark.mistwave,
    secondary = secondaryDark.mistwave,
    onSecondary = onSecondaryDark.mistwave,
    secondaryContainer = secondaryContainerDark.mistwave,
    onSecondaryContainer = onSecondaryContainerDark.mistwave,
    tertiary = tertiaryDark.mistwave,
    onTertiary = onTertiaryDark.mistwave,
    tertiaryContainer = tertiaryContainerDark.mistwave,
    onTertiaryContainer = onTertiaryContainerDark.mistwave,
    error = errorDark.mistwave,
    onError = onErrorDark.mistwave,
    errorContainer = errorContainerDark.mistwave,
    onErrorContainer = onErrorContainerDark.mistwave,
    background = backgroundDark.mistwave,
    onBackground = onBackgroundDark.mistwave,
    surface = surfaceDark.mistwave,
    onSurface = onSurfaceDark.mistwave,
    surfaceVariant = surfaceVariantDark.mistwave,
    onSurfaceVariant = onSurfaceVariantDark.mistwave,
    outline = outlineDark.mistwave,
    outlineVariant = outlineVariantDark.mistwave,
    scrim = scrimDark.mistwave,
    inverseSurface = inverseSurfaceDark.mistwave,
    inverseOnSurface = inverseOnSurfaceDark.mistwave,
    inversePrimary = inversePrimaryDark.mistwave,
    surfaceDim = surfaceDimDark.mistwave,
    surfaceBright = surfaceBrightDark.mistwave,
    surfaceContainerLowest = surfaceContainerLowestDark.mistwave,
    surfaceContainerLow = surfaceContainerLowDark.mistwave,
    surfaceContainer = surfaceContainerDark.mistwave,
    surfaceContainerHigh = surfaceContainerHighDark.mistwave,
    surfaceContainerHighest = surfaceContainerHighestDark.mistwave,
)

private val glacierLight = lightColorScheme(
    primary = primaryLight.glacier,
    onPrimary = onPrimaryLight.glacier,
    primaryContainer = primaryContainerLight.glacier,
    onPrimaryContainer = onPrimaryContainerLight.glacier,
    secondary = secondaryLight.glacier,
    onSecondary = onSecondaryLight.glacier,
    secondaryContainer = secondaryContainerLight.glacier,
    onSecondaryContainer = onSecondaryContainerLight.glacier,
    tertiary = tertiaryLight.glacier,
    onTertiary = onTertiaryLight.glacier,
    tertiaryContainer = tertiaryContainerLight.glacier,
    onTertiaryContainer = onTertiaryContainerLight.glacier,
    error = errorLight.glacier,
    onError = onErrorLight.glacier,
    errorContainer = errorContainerLight.glacier,
    onErrorContainer = onErrorContainerLight.glacier,
    background = backgroundLight.glacier,
    onBackground = onBackgroundLight.glacier,
    surface = surfaceLight.glacier,
    onSurface = onSurfaceLight.glacier,
    surfaceVariant = surfaceVariantLight.glacier,
    onSurfaceVariant = onSurfaceVariantLight.glacier,
    outline = outlineLight.glacier,
    outlineVariant = outlineVariantLight.glacier,
    scrim = scrimLight.glacier,
    inverseSurface = inverseSurfaceLight.glacier,
    inverseOnSurface = inverseOnSurfaceLight.glacier,
    inversePrimary = inversePrimaryLight.glacier,
    surfaceDim = surfaceDimLight.glacier,
    surfaceBright = surfaceBrightLight.glacier,
    surfaceContainerLowest = surfaceContainerLowestLight.glacier,
    surfaceContainerLow = surfaceContainerLowLight.glacier,
    surfaceContainer = surfaceContainerLight.glacier,
    surfaceContainerHigh = surfaceContainerHighLight.glacier,
    surfaceContainerHighest = surfaceContainerHighestLight.glacier,
)

private val glacierDark = darkColorScheme(
    primary = primaryDark.glacier,
    onPrimary = onPrimaryDark.glacier,
    primaryContainer = primaryContainerDark.glacier,
    onPrimaryContainer = onPrimaryContainerDark.glacier,
    secondary = secondaryDark.glacier,
    onSecondary = onSecondaryDark.glacier,
    secondaryContainer = secondaryContainerDark.glacier,
    onSecondaryContainer = onSecondaryContainerDark.glacier,
    tertiary = tertiaryDark.glacier,
    onTertiary = onTertiaryDark.glacier,
    tertiaryContainer = tertiaryContainerDark.glacier,
    onTertiaryContainer = onTertiaryContainerDark.glacier,
    error = errorDark.glacier,
    onError = onErrorDark.glacier,
    errorContainer = errorContainerDark.glacier,
    onErrorContainer = onErrorContainerDark.glacier,
    background = backgroundDark.glacier,
    onBackground = onBackgroundDark.glacier,
    surface = surfaceDark.glacier,
    onSurface = onSurfaceDark.glacier,
    surfaceVariant = surfaceVariantDark.glacier,
    onSurfaceVariant = onSurfaceVariantDark.glacier,
    outline = outlineDark.glacier,
    outlineVariant = outlineVariantDark.glacier,
    scrim = scrimDark.glacier,
    inverseSurface = inverseSurfaceDark.glacier,
    inverseOnSurface = inverseOnSurfaceDark.glacier,
    inversePrimary = inversePrimaryDark.glacier,
    surfaceDim = surfaceDimDark.glacier,
    surfaceBright = surfaceBrightDark.glacier,
    surfaceContainerLowest = surfaceContainerLowestDark.glacier,
    surfaceContainerLow = surfaceContainerLowDark.glacier,
    surfaceContainer = surfaceContainerDark.glacier,
    surfaceContainerHigh = surfaceContainerHighDark.glacier,
    surfaceContainerHighest = surfaceContainerHighestDark.glacier,
)

private val verdantFieldLight = lightColorScheme(
    primary = primaryLight.verdantField,
    onPrimary = onPrimaryLight.verdantField,
    primaryContainer = primaryContainerLight.verdantField,
    onPrimaryContainer = onPrimaryContainerLight.verdantField,
    secondary = secondaryLight.verdantField,
    onSecondary = onSecondaryLight.verdantField,
    secondaryContainer = secondaryContainerLight.verdantField,
    onSecondaryContainer = onSecondaryContainerLight.verdantField,
    tertiary = tertiaryLight.verdantField,
    onTertiary = onTertiaryLight.verdantField,
    tertiaryContainer = tertiaryContainerLight.verdantField,
    onTertiaryContainer = onTertiaryContainerLight.verdantField,
    error = errorLight.verdantField,
    onError = onErrorLight.verdantField,
    errorContainer = errorContainerLight.verdantField,
    onErrorContainer = onErrorContainerLight.verdantField,
    background = backgroundLight.verdantField,
    onBackground = onBackgroundLight.verdantField,
    surface = surfaceLight.verdantField,
    onSurface = onSurfaceLight.verdantField,
    surfaceVariant = surfaceVariantLight.verdantField,
    onSurfaceVariant = onSurfaceVariantLight.verdantField,
    outline = outlineLight.verdantField,
    outlineVariant = outlineVariantLight.verdantField,
    scrim = scrimLight.verdantField,
    inverseSurface = inverseSurfaceLight.verdantField,
    inverseOnSurface = inverseOnSurfaceLight.verdantField,
    inversePrimary = inversePrimaryLight.verdantField,
    surfaceDim = surfaceDimLight.verdantField,
    surfaceBright = surfaceBrightLight.verdantField,
    surfaceContainerLowest = surfaceContainerLowestLight.verdantField,
    surfaceContainerLow = surfaceContainerLowLight.verdantField,
    surfaceContainer = surfaceContainerLight.verdantField,
    surfaceContainerHigh = surfaceContainerHighLight.verdantField,
    surfaceContainerHighest = surfaceContainerHighestLight.verdantField,
)

private val verdantFieldDark = darkColorScheme(
    primary = primaryDark.verdantField,
    onPrimary = onPrimaryDark.verdantField,
    primaryContainer = primaryContainerDark.verdantField,
    onPrimaryContainer = onPrimaryContainerDark.verdantField,
    secondary = secondaryDark.verdantField,
    onSecondary = onSecondaryDark.verdantField,
    secondaryContainer = secondaryContainerDark.verdantField,
    onSecondaryContainer = onSecondaryContainerDark.verdantField,
    tertiary = tertiaryDark.verdantField,
    onTertiary = onTertiaryDark.verdantField,
    tertiaryContainer = tertiaryContainerDark.verdantField,
    onTertiaryContainer = onTertiaryContainerDark.verdantField,
    error = errorDark.verdantField,
    onError = onErrorDark.verdantField,
    errorContainer = errorContainerDark.verdantField,
    onErrorContainer = onErrorContainerDark.verdantField,
    background = backgroundDark.verdantField,
    onBackground = onBackgroundDark.verdantField,
    surface = surfaceDark.verdantField,
    onSurface = onSurfaceDark.verdantField,
    surfaceVariant = surfaceVariantDark.verdantField,
    onSurfaceVariant = onSurfaceVariantDark.verdantField,
    outline = outlineDark.verdantField,
    outlineVariant = outlineVariantDark.verdantField,
    scrim = scrimDark.verdantField,
    inverseSurface = inverseSurfaceDark.verdantField,
    inverseOnSurface = inverseOnSurfaceDark.verdantField,
    inversePrimary = inversePrimaryDark.verdantField,
    surfaceDim = surfaceDimDark.verdantField,
    surfaceBright = surfaceBrightDark.verdantField,
    surfaceContainerLowest = surfaceContainerLowestDark.verdantField,
    surfaceContainerLow = surfaceContainerLowDark.verdantField,
    surfaceContainer = surfaceContainerDark.verdantField,
    surfaceContainerHigh = surfaceContainerHighDark.verdantField,
    surfaceContainerHighest = surfaceContainerHighestDark.verdantField,
)

private val urbanAshLight = lightColorScheme(
    primary = primaryLight.urbanAsh,
    onPrimary = onPrimaryLight.urbanAsh,
    primaryContainer = primaryContainerLight.urbanAsh,
    onPrimaryContainer = onPrimaryContainerLight.urbanAsh,
    secondary = secondaryLight.urbanAsh,
    onSecondary = onSecondaryLight.urbanAsh,
    secondaryContainer = secondaryContainerLight.urbanAsh,
    onSecondaryContainer = onSecondaryContainerLight.urbanAsh,
    tertiary = tertiaryLight.urbanAsh,
    onTertiary = onTertiaryLight.urbanAsh,
    tertiaryContainer = tertiaryContainerLight.urbanAsh,
    onTertiaryContainer = onTertiaryContainerLight.urbanAsh,
    error = errorLight.urbanAsh,
    onError = onErrorLight.urbanAsh,
    errorContainer = errorContainerLight.urbanAsh,
    onErrorContainer = onErrorContainerLight.urbanAsh,
    background = backgroundLight.urbanAsh,
    onBackground = onBackgroundLight.urbanAsh,
    surface = surfaceLight.urbanAsh,
    onSurface = onSurfaceLight.urbanAsh,
    surfaceVariant = surfaceVariantLight.urbanAsh,
    onSurfaceVariant = onSurfaceVariantLight.urbanAsh,
    outline = outlineLight.urbanAsh,
    outlineVariant = outlineVariantLight.urbanAsh,
    scrim = scrimLight.urbanAsh,
    inverseSurface = inverseSurfaceLight.urbanAsh,
    inverseOnSurface = inverseOnSurfaceLight.urbanAsh,
    inversePrimary = inversePrimaryLight.urbanAsh,
    surfaceDim = surfaceDimLight.urbanAsh,
    surfaceBright = surfaceBrightLight.urbanAsh,
    surfaceContainerLowest = surfaceContainerLowestLight.urbanAsh,
    surfaceContainerLow = surfaceContainerLowLight.urbanAsh,
    surfaceContainer = surfaceContainerLight.urbanAsh,
    surfaceContainerHigh = surfaceContainerHighLight.urbanAsh,
    surfaceContainerHighest = surfaceContainerHighestLight.urbanAsh,
)

private val urbanAshDark = darkColorScheme(
    primary = primaryDark.urbanAsh,
    onPrimary = onPrimaryDark.urbanAsh,
    primaryContainer = primaryContainerDark.urbanAsh,
    onPrimaryContainer = onPrimaryContainerDark.urbanAsh,
    secondary = secondaryDark.urbanAsh,
    onSecondary = onSecondaryDark.urbanAsh,
    secondaryContainer = secondaryContainerDark.urbanAsh,
    onSecondaryContainer = onSecondaryContainerDark.urbanAsh,
    tertiary = tertiaryDark.urbanAsh,
    onTertiary = onTertiaryDark.urbanAsh,
    tertiaryContainer = tertiaryContainerDark.urbanAsh,
    onTertiaryContainer = onTertiaryContainerDark.urbanAsh,
    error = errorDark.urbanAsh,
    onError = onErrorDark.urbanAsh,
    errorContainer = errorContainerDark.urbanAsh,
    onErrorContainer = onErrorContainerDark.urbanAsh,
    background = backgroundDark.urbanAsh,
    onBackground = onBackgroundDark.urbanAsh,
    surface = surfaceDark.urbanAsh,
    onSurface = onSurfaceDark.urbanAsh,
    surfaceVariant = surfaceVariantDark.urbanAsh,
    onSurfaceVariant = onSurfaceVariantDark.urbanAsh,
    outline = outlineDark.urbanAsh,
    outlineVariant = outlineVariantDark.urbanAsh,
    scrim = scrimDark.urbanAsh,
    inverseSurface = inverseSurfaceDark.urbanAsh,
    inverseOnSurface = inverseOnSurfaceDark.urbanAsh,
    inversePrimary = inversePrimaryDark.urbanAsh,
    surfaceDim = surfaceDimDark.urbanAsh,
    surfaceBright = surfaceBrightDark.urbanAsh,
    surfaceContainerLowest = surfaceContainerLowestDark.urbanAsh,
    surfaceContainerLow = surfaceContainerLowDark.urbanAsh,
    surfaceContainer = surfaceContainerDark.urbanAsh,
    surfaceContainerHigh = surfaceContainerHighDark.urbanAsh,
    surfaceContainerHighest = surfaceContainerHighestDark.urbanAsh,
)

private val verdantDawnLight = lightColorScheme(
    primary = primaryLight.verdantDawn,
    onPrimary = onPrimaryLight.verdantDawn,
    primaryContainer = primaryContainerLight.verdantDawn,
    onPrimaryContainer = onPrimaryContainerLight.verdantDawn,
    secondary = secondaryLight.verdantDawn,
    onSecondary = onSecondaryLight.verdantDawn,
    secondaryContainer = secondaryContainerLight.verdantDawn,
    onSecondaryContainer = onSecondaryContainerLight.verdantDawn,
    tertiary = tertiaryLight.verdantDawn,
    onTertiary = onTertiaryLight.verdantDawn,
    tertiaryContainer = tertiaryContainerLight.verdantDawn,
    onTertiaryContainer = onTertiaryContainerLight.verdantDawn,
    error = errorLight.verdantDawn,
    onError = onErrorLight.verdantDawn,
    errorContainer = errorContainerLight.verdantDawn,
    onErrorContainer = onErrorContainerLight.verdantDawn,
    background = backgroundLight.verdantDawn,
    onBackground = onBackgroundLight.verdantDawn,
    surface = surfaceLight.verdantDawn,
    onSurface = onSurfaceLight.verdantDawn,
    surfaceVariant = surfaceVariantLight.verdantDawn,
    onSurfaceVariant = onSurfaceVariantLight.verdantDawn,
    outline = outlineLight.verdantDawn,
    outlineVariant = outlineVariantLight.verdantDawn,
    scrim = scrimLight.verdantDawn,
    inverseSurface = inverseSurfaceLight.verdantDawn,
    inverseOnSurface = inverseOnSurfaceLight.verdantDawn,
    inversePrimary = inversePrimaryLight.verdantDawn,
    surfaceDim = surfaceDimLight.verdantDawn,
    surfaceBright = surfaceBrightLight.verdantDawn,
    surfaceContainerLowest = surfaceContainerLowestLight.verdantDawn,
    surfaceContainerLow = surfaceContainerLowLight.verdantDawn,
    surfaceContainer = surfaceContainerLight.verdantDawn,
    surfaceContainerHigh = surfaceContainerHighLight.verdantDawn,
    surfaceContainerHighest = surfaceContainerHighestLight.verdantDawn,
)

private val verdantDawnDark = darkColorScheme(
    primary = primaryDark.verdantDawn,
    onPrimary = onPrimaryDark.verdantDawn,
    primaryContainer = primaryContainerDark.verdantDawn,
    onPrimaryContainer = onPrimaryContainerDark.verdantDawn,
    secondary = secondaryDark.verdantDawn,
    onSecondary = onSecondaryDark.verdantDawn,
    secondaryContainer = secondaryContainerDark.verdantDawn,
    onSecondaryContainer = onSecondaryContainerDark.verdantDawn,
    tertiary = tertiaryDark.verdantDawn,
    onTertiary = onTertiaryDark.verdantDawn,
    tertiaryContainer = tertiaryContainerDark.verdantDawn,
    onTertiaryContainer = onTertiaryContainerDark.verdantDawn,
    error = errorDark.verdantDawn,
    onError = onErrorDark.verdantDawn,
    errorContainer = errorContainerDark.verdantDawn,
    onErrorContainer = onErrorContainerDark.verdantDawn,
    background = backgroundDark.verdantDawn,
    onBackground = onBackgroundDark.verdantDawn,
    surface = surfaceDark.verdantDawn,
    onSurface = onSurfaceDark.verdantDawn,
    surfaceVariant = surfaceVariantDark.verdantDawn,
    onSurfaceVariant = onSurfaceVariantDark.verdantDawn,
    outline = outlineDark.verdantDawn,
    outlineVariant = outlineVariantDark.verdantDawn,
    scrim = scrimDark.verdantDawn,
    inverseSurface = inverseSurfaceDark.verdantDawn,
    inverseOnSurface = inverseOnSurfaceDark.verdantDawn,
    inversePrimary = inversePrimaryDark.verdantDawn,
    surfaceDim = surfaceDimDark.verdantDawn,
    surfaceBright = surfaceBrightDark.verdantDawn,
    surfaceContainerLowest = surfaceContainerLowestDark.verdantDawn,
    surfaceContainerLow = surfaceContainerLowDark.verdantDawn,
    surfaceContainer = surfaceContainerDark.verdantDawn,
    surfaceContainerHigh = surfaceContainerHighDark.verdantDawn,
    surfaceContainerHighest = surfaceContainerHighestDark.verdantDawn,
)

@SuppressLint("RestrictedApi")
private fun customLight(color: Color): ColorScheme {
    val scheme = Scheme.light(color.toArgb())
    val hctNeutral = Hct.fromInt(scheme.surface)
    //https://github.com/flutter/flutter/issues/137679
    val surfaceDim               = Hct.from(hctNeutral.hue, hctNeutral.chroma, 87.0).toInt()
    val surfaceBright            = Hct.from(hctNeutral.hue, hctNeutral.chroma, 98.0).toInt()
    val surfaceContainerLowest   = Hct.from(hctNeutral.hue, hctNeutral.chroma, 100.0).toInt()
    val surfaceContainerLow      = Hct.from(hctNeutral.hue, hctNeutral.chroma, 96.0).toInt()
    val surfaceContainer         = Hct.from(hctNeutral.hue, hctNeutral.chroma, 94.0).toInt()
    val surfaceContainerHigh     = Hct.from(hctNeutral.hue, hctNeutral.chroma, 92.0).toInt()
    val surfaceContainerHighest  = Hct.from(hctNeutral.hue, hctNeutral.chroma, 90.0).toInt()

    return lightColorScheme(
        primary = Color(scheme.primary),
        onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        secondary = Color(scheme.secondary),
        onSecondary = Color(scheme.onSecondary),
        secondaryContainer = Color(scheme.secondaryContainer),
        onSecondaryContainer = Color(scheme.onSecondaryContainer),
        tertiary = Color(scheme.tertiary),
        onTertiary = Color(scheme.onTertiary),
        tertiaryContainer = Color(scheme.tertiaryContainer),
        onTertiaryContainer = Color(scheme.onTertiaryContainer),
        error = Color(scheme.error),
        onError = Color(scheme.onError),
        errorContainer = Color(scheme.errorContainer),
        onErrorContainer = Color(scheme.onErrorContainer),
        background = Color(scheme.background),
        onBackground = Color(scheme.onBackground),
        surface = Color(scheme.surface),
        onSurface = Color(scheme.onSurface),
        surfaceVariant = Color(scheme.surfaceVariant),
        onSurfaceVariant = Color(scheme.onSurfaceVariant),
        outline = Color(scheme.outline),
        outlineVariant = Color(scheme.outlineVariant),
        scrim = Color(scheme.scrim),
        inverseSurface = Color(scheme.inverseSurface),
        inverseOnSurface = Color(scheme.inverseOnSurface),
        inversePrimary = Color(scheme.inversePrimary),
        surfaceDim = Color(surfaceDim),
        surfaceBright = Color(surfaceBright),
        surfaceContainerLowest = Color(surfaceContainerLowest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest)
    )
}

@SuppressLint("RestrictedApi")
private fun customDark(color: Color): ColorScheme {
    val scheme = Scheme.dark(color.toArgb())
    val hctNeutral = Hct.fromInt(scheme.surface)
    //https://github.com/flutter/flutter/issues/137679
    val surfaceDim                = Hct.from(hctNeutral.hue, hctNeutral.chroma, 6.0).toInt()
    val surfaceBright             = Hct.from(hctNeutral.hue, hctNeutral.chroma, 24.0).toInt()
    val surfaceContainerLowest    = Hct.from(hctNeutral.hue, hctNeutral.chroma, 4.0).toInt()
    val surfaceContainerLow       = Hct.from(hctNeutral.hue, hctNeutral.chroma, 10.0).toInt()
    val surfaceContainer          = Hct.from(hctNeutral.hue, hctNeutral.chroma, 12.0).toInt()
    val surfaceContainerHigh      = Hct.from(hctNeutral.hue, hctNeutral.chroma, 17.0).toInt()
    val surfaceContainerHighest   = Hct.from(hctNeutral.hue, hctNeutral.chroma, 22.0).toInt()

    return lightColorScheme(
        primary = Color(scheme.primary),
        onPrimary = Color(scheme.onPrimary),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        secondary = Color(scheme.secondary),
        onSecondary = Color(scheme.onSecondary),
        secondaryContainer = Color(scheme.secondaryContainer),
        onSecondaryContainer = Color(scheme.onSecondaryContainer),
        tertiary = Color(scheme.tertiary),
        onTertiary = Color(scheme.onTertiary),
        tertiaryContainer = Color(scheme.tertiaryContainer),
        onTertiaryContainer = Color(scheme.onTertiaryContainer),
        error = Color(scheme.error),
        onError = Color(scheme.onError),
        errorContainer = Color(scheme.errorContainer),
        onErrorContainer = Color(scheme.onErrorContainer),
        background = Color(scheme.background),
        onBackground = Color(scheme.onBackground),
        surface = Color(scheme.surface),
        onSurface = Color(scheme.onSurface),
        surfaceVariant = Color(scheme.surfaceVariant),
        onSurfaceVariant = Color(scheme.onSurfaceVariant),
        outline = Color(scheme.outline),
        outlineVariant = Color(scheme.outlineVariant),
        scrim = Color(scheme.scrim),
        inverseSurface = Color(scheme.inverseSurface),
        inverseOnSurface = Color(scheme.inverseOnSurface),
        inversePrimary = Color(scheme.inversePrimary),
        surfaceDim = Color(surfaceDim),
        surfaceBright = Color(surfaceBright),
        surfaceContainerLowest = Color(surfaceContainerLowest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest)
    )
}

@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    @Composable
    fun getColorAsState(targetValue: Color): State<Color> = animateColorAsState(
        targetValue = targetValue,
        label = "ThemeTransition",
        animationSpec = getAnimateTween()
    )

    return ColorScheme(
        primary = getColorAsState(targetValue = target.primary).value,
        onPrimary = getColorAsState(targetValue = target.onPrimary).value,
        primaryContainer = getColorAsState(targetValue = target.primaryContainer).value,
        onPrimaryContainer = getColorAsState(targetValue = target.onPrimaryContainer).value,
        inversePrimary = getColorAsState(targetValue = target.inversePrimary).value,
        secondary = getColorAsState(targetValue = target.secondary).value,
        onSecondary = getColorAsState(targetValue = target.onSecondary).value,
        secondaryContainer = getColorAsState(targetValue = target.secondaryContainer).value,
        onSecondaryContainer = getColorAsState(targetValue = target.onSecondaryContainer).value,
        tertiary = getColorAsState(targetValue = target.tertiary).value,
        onTertiary = getColorAsState(targetValue = target.onTertiary).value,
        tertiaryContainer = getColorAsState(targetValue = target.tertiaryContainer).value,
        onTertiaryContainer = getColorAsState(targetValue = target.onTertiaryContainer).value,
        background = getColorAsState(targetValue = target.background).value,
        onBackground = getColorAsState(targetValue = target.onBackground).value,
        surface = getColorAsState(targetValue = target.surface).value,
        onSurface = getColorAsState(targetValue = target.onSurface).value,
        surfaceVariant = getColorAsState(targetValue = target.surfaceVariant).value,
        onSurfaceVariant = getColorAsState(targetValue = target.onSurfaceVariant).value,
        surfaceTint = getColorAsState(targetValue = target.surfaceTint).value,
        inverseSurface = getColorAsState(targetValue = target.inverseSurface).value,
        inverseOnSurface = getColorAsState(targetValue = target.inverseOnSurface).value,
        error = getColorAsState(targetValue = target.error).value,
        onError = getColorAsState(targetValue = target.onError).value,
        errorContainer = getColorAsState(targetValue = target.errorContainer).value,
        onErrorContainer = getColorAsState(targetValue = target.onErrorContainer).value,
        outline = getColorAsState(targetValue = target.outline).value,
        outlineVariant = getColorAsState(targetValue = target.outlineVariant).value,
        scrim = getColorAsState(targetValue = target.scrim).value,
        surfaceBright = getColorAsState(targetValue = target.surfaceBright).value,
        surfaceContainer = getColorAsState(targetValue = target.surfaceContainer).value,
        surfaceContainerHigh = getColorAsState(targetValue = target.surfaceContainerHigh).value,
        surfaceContainerHighest = getColorAsState(targetValue = target.surfaceContainerHighest).value,
        surfaceContainerLow = getColorAsState(targetValue = target.surfaceContainerLow).value,
        surfaceContainerLowest = getColorAsState(targetValue = target.surfaceContainerLowest).value,
        surfaceDim = getColorAsState(targetValue = target.surfaceDim).value,
    )
}

@Composable
fun ZalithLauncherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorThemeState = remember { ColorThemeState() }
    val customColorThemeState = remember { CustomColorThemeState() }

    CompositionLocalProvider(
        LocalColorThemeState provides colorThemeState,
        LocalCustomColorThemeState provides customColorThemeState
    ) {
        val currentColorTheme by LocalColorThemeState.current
        val currentCustomColor by LocalCustomColorThemeState.current

        val colorScheme = when {
            dynamicColor && currentColorTheme == ColorThemeType.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> when (currentColorTheme) {
                ColorThemeType.EMBERMIRE -> embermireDark
                ColorThemeType.VELVET_ROSE -> velvetRoseDark
                ColorThemeType.MISTWAVE -> mistwaveDark
                ColorThemeType.GLACIER -> glacierDark
                ColorThemeType.VERDANTFIELD -> verdantFieldDark
                ColorThemeType.URBAN_ASH -> urbanAshDark
                ColorThemeType.VERDANT_DAWN -> verdantDawnDark
                ColorThemeType.CUSTOM -> customDark(currentCustomColor)
                else -> embermireDark
            }
            else -> when (currentColorTheme) {
                ColorThemeType.EMBERMIRE -> embermireLight
                ColorThemeType.VELVET_ROSE -> velvetRoseLight
                ColorThemeType.MISTWAVE -> mistwaveLight
                ColorThemeType.GLACIER -> glacierLight
                ColorThemeType.VERDANTFIELD -> verdantFieldLight
                ColorThemeType.URBAN_ASH -> urbanAshLight
                ColorThemeType.VERDANT_DAWN -> verdantDawnLight
                ColorThemeType.CUSTOM -> customLight(currentCustomColor)
                else -> embermireLight
            }
        }

        val animateColorScheme = animateColorScheme(colorScheme)

        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as Activity).window
                window.statusBarColor = animateColorScheme.primary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
            }
        }

        MaterialTheme(
            colorScheme = animateColorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
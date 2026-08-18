package com.abook.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ABookDarkColorScheme = darkColorScheme(
    primary = OrangeAccent,
    onPrimary = PureBlack,
    secondary = OrangeGlow,
    background = PureBlack,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnBackgroundDark,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = OnSurfaceVariantDark,
)

private val ABookLightColorScheme = lightColorScheme(
    primary = OrangeAccentDark,
    onPrimary = SurfaceLight,
    secondary = OrangeAccent,
    background = WarmWhite,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnBackgroundLight,
    surfaceVariant = WarmWhite,
    onSurfaceVariant = OnSurfaceVariantLight,
)

@Composable
fun ABookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ABookDarkColorScheme else ABookLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ABookTypography,
        content = content
    )
}

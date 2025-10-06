package com.aljoud.shifts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors: ColorScheme = lightColorScheme(
    primary = Gold,
    onPrimary = NavyDark,
    primaryContainer = Gold,
    onPrimaryContainer = NavyDark,

    secondary = Navy,
    onSecondary = OnNavy,
    secondaryContainer = NavyDark,
    onSecondaryContainer = OnNavy,

    background = Cream,
    onBackground = NavyDark,
    surface = SurfaceLt,
    onSurface = NavyDark
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Gold_L,
    onPrimary = OnSurfaceD,
    primaryContainer = Navy_L,
    onPrimaryContainer = OnSurfaceD,

    secondary = Navy_L,
    onSecondary = OnSurfaceD,
    secondaryContainer = Navy_L,
    onSecondaryContainer = OnSurfaceD,

    background = SurfaceDk,
    onBackground = OnSurfaceD,
    surface = SurfaceDk,
    onSurface = OnSurfaceD
)

@Composable
fun AlJoudShiftsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = Shapes(),
        content = content
    )
}

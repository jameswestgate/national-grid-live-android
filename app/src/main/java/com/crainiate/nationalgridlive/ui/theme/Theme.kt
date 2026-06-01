package com.crainiate.nationalgridlive.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = md_primary_light,
    onPrimary = md_onPrimary_light,
    primaryContainer = md_primaryContainer_light,
    onPrimaryContainer = md_onPrimaryContainer_light,
    secondaryContainer = md_secondaryContainer_light,
    onSecondaryContainer = md_onSecondaryContainer_light,
    background = md_background_light,
    onBackground = md_onSurface_light,
    surface = md_surface_light,
    onSurface = md_onSurface_light,
    surfaceVariant = md_surfaceVariant_light,
    onSurfaceVariant = md_onSurfaceVariant_light,
    outlineVariant = md_outlineVariant_light,
    surfaceContainerLowest = md_surfaceContainerLowest_light,
    surfaceContainerLow = md_surfaceContainerLow_light,
    surfaceContainer = md_surfaceContainer_light,
    surfaceContainerHigh = md_surfaceContainerHigh_light,
    surfaceContainerHighest = md_surfaceContainerHighest_light,
)

private val DarkColors = darkColorScheme(
    primary = md_primary_dark,
    onPrimary = md_onPrimary_dark,
    primaryContainer = md_primaryContainer_dark,
    onPrimaryContainer = md_onPrimaryContainer_dark,
    secondaryContainer = md_secondaryContainer_dark,
    onSecondaryContainer = md_onSecondaryContainer_dark,
    background = md_background_dark,
    onBackground = md_onSurface_dark,
    surface = md_surface_dark,
    onSurface = md_onSurface_dark,
    surfaceVariant = md_surfaceVariant_dark,
    onSurfaceVariant = md_onSurfaceVariant_dark,
    outlineVariant = md_outlineVariant_dark,
    surfaceContainerLowest = md_surfaceContainerLowest_dark,
    surfaceContainerLow = md_surfaceContainerLow_dark,
    surfaceContainer = md_surfaceContainer_dark,
    surfaceContainerHigh = md_surfaceContainerHigh_dark,
    surfaceContainerHighest = md_surfaceContainerHighest_dark,
)

@Composable
fun NationalGridLiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Material You wallpaper tinting on Android 12+. Off by default so the brand
    // green scheme shows; flip to true to follow the user's wallpaper.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

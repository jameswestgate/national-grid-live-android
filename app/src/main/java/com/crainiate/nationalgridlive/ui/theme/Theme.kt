package com.crainiate.nationalgridlive.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import com.crainiate.nationalgridlive.data.settings.AppColorScheme

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

/**
 * Take the HUE from `this` (the wallpaper-derived Material You colour) but keep
 * the SATURATION + LIGHTNESS of `ref` (the brand-green token). So the app follows
 * the user's wallpaper *hue* while every tint stays exactly as subtle/intense as
 * the brand green — no loud, fully-saturated wallpaper colours.
 */
private fun Color.temperedTo(ref: Color): Color {
    val dyn = FloatArray(3).also { ColorUtils.colorToHSL(this.toArgb(), it) }
    val brand = FloatArray(3).also { ColorUtils.colorToHSL(ref.toArgb(), it) }
    // h from dynamic, s + l from brand.
    return Color(ColorUtils.HSLToColor(floatArrayOf(dyn[0], brand[1], brand[2])))
}

/**
 * A "tempered" Material You scheme: surfaces, containers and the accent follow
 * the wallpaper hue but at the brand green's saturation/lightness; the text /
 * contrast roles (`on*`, outline) are kept from the brand scheme so legibility
 * is identical to the fixed theme. Fuel/data colours are unaffected (FuelColors).
 */
private fun temperedScheme(dynamic: ColorScheme, brand: ColorScheme): ColorScheme =
    dynamic.copy(
        primary = dynamic.primary.temperedTo(brand.primary),
        primaryContainer = dynamic.primaryContainer.temperedTo(brand.primaryContainer),
        secondaryContainer = dynamic.secondaryContainer.temperedTo(brand.secondaryContainer),
        background = dynamic.background.temperedTo(brand.background),
        surface = dynamic.surface.temperedTo(brand.surface),
        surfaceVariant = dynamic.surfaceVariant.temperedTo(brand.surfaceVariant),
        surfaceContainerLowest = dynamic.surfaceContainerLowest.temperedTo(brand.surfaceContainerLowest),
        surfaceContainerLow = dynamic.surfaceContainerLow.temperedTo(brand.surfaceContainerLow),
        surfaceContainer = dynamic.surfaceContainer.temperedTo(brand.surfaceContainer),
        surfaceContainerHigh = dynamic.surfaceContainerHigh.temperedTo(brand.surfaceContainerHigh),
        surfaceContainerHighest = dynamic.surfaceContainerHighest.temperedTo(brand.surfaceContainerHighest),
        // Keep the brand's text / contrast colours so legibility matches the app.
        onPrimary = brand.onPrimary,
        onPrimaryContainer = brand.onPrimaryContainer,
        onSecondaryContainer = brand.onSecondaryContainer,
        onBackground = brand.onBackground,
        onSurface = brand.onSurface,
        onSurfaceVariant = brand.onSurfaceVariant,
        outlineVariant = brand.outlineVariant,
    )

/* ---------------- fixed preset schemes (Sage / Grey) ----------------
 * Re-tints of the brand scheme for the user-selectable "Theme colour" setting. */

private fun Color.shifted(hue: Float?, satScale: Float): Color {
    val hsl = FloatArray(3).also { ColorUtils.colorToHSL(this.toArgb(), it) }
    return Color(
        ColorUtils.HSLToColor(
            floatArrayOf(hue ?: hsl[0], (hsl[1] * satScale).coerceIn(0f, 1f), hsl[2])
        )
    )
}

/** Re-tint the brand scheme's surface/accent roles (hue and/or saturation), keeping
 *  the text/contrast roles so legibility is unchanged. */
private fun transformScheme(base: ColorScheme, hue: Float?, satScale: Float): ColorScheme =
    base.copy(
        primary = base.primary.shifted(hue, satScale),
        primaryContainer = base.primaryContainer.shifted(hue, satScale),
        secondaryContainer = base.secondaryContainer.shifted(hue, satScale),
        background = base.background.shifted(hue, satScale),
        surface = base.surface.shifted(hue, satScale),
        surfaceVariant = base.surfaceVariant.shifted(hue, satScale),
        surfaceContainerLowest = base.surfaceContainerLowest.shifted(hue, satScale),
        surfaceContainerLow = base.surfaceContainerLow.shifted(hue, satScale),
        surfaceContainer = base.surfaceContainer.shifted(hue, satScale),
        surfaceContainerHigh = base.surfaceContainerHigh.shifted(hue, satScale),
        surfaceContainerHighest = base.surfaceContainerHighest.shifted(hue, satScale),
    )

/**
 * The live Material 3 colour scheme for a given [AppColorScheme] (the user's
 * "Theme colour" setting) and dark/light flag. Drives both the app (via
 * [NationalGridLiveTheme]) and the home-screen widget background, so they always
 * match. Fuel/data colours (FuelColors) are independent of this.
 *  - Green     → the fixed brand scheme
 *  - Sage/Grey → re-tints of the brand scheme (transformScheme)
 *  - Wallpaper → tempered Material You on Android 12+ (else brand green)
 */
fun appColorScheme(scheme: AppColorScheme, darkTheme: Boolean, context: Context): ColorScheme {
    val base = if (darkTheme) DarkColors else LightColors
    return when (scheme) {
        AppColorScheme.Green -> base
        AppColorScheme.Sage -> transformScheme(base, hue = 168f, satScale = 0.85f)
        AppColorScheme.Grey -> transformScheme(base, hue = null, satScale = 0f)
        AppColorScheme.Dynamic ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val dynamic =
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                temperedScheme(dynamic, base)
            } else {
                base // no Material You below Android 12 → brand green
            }
    }
}

@Composable
fun NationalGridLiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: AppColorScheme = AppColorScheme.Green,
    content: @Composable () -> Unit
) {
    val scheme = appColorScheme(colorScheme, darkTheme, LocalContext.current)
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}

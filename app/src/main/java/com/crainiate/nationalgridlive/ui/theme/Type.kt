package com.crainiate.nationalgridlive.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Material 3 type scale. Uses the platform default (Roboto on Android). Only the
 * roles the app leans on are tuned; the rest fall back to Material defaults.
 */
private val def = Typography()

val AppTypography = Typography(
    headlineMedium = def.headlineMedium.copy(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium
    ),
    titleLarge = def.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = def.titleMedium.copy(fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)

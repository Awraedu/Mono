package com.awras.mono.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Dark Palette (Primary Target for Datum)
val SurfaceDark = Color(0xFF000000)          // Pure pitch black background
val CardSurfaceDark = Color(0xFF0C0C0E)      // Subtle elevated surface
val SurfaceVariantDark = Color(0xFF16161A)   // Clean neutral dark surface variant (No pinkish tint)
val CardBorderDark = Color(0xFF222225)       // Hairline border stroke (1px)
val TextPrimaryDark = Color(0xFFFFFFFF)      // Pure crisp white
val TextSecondaryDark = Color(0xFF8E8E93)    // Muted slate gray
val TextTertiaryDark = Color(0xFF48484A)     // Low-contrast technical metadata

// Light Palette
val SurfaceLight = Color(0xFFF9F9FB)         // Off-white paper background
val CardSurfaceLight = Color(0xFFFFFFFF)     // Crisp white surface
val SurfaceVariantLight = Color(0xFFEFEFF4)  // Clean neutral light surface variant
val CardBorderLight = Color(0xFFE5E5EA)      // Soft gray border stroke
val TextPrimaryLight = Color(0xFF000000)     // Pure black
val TextSecondaryLight = Color(0xFF636366)   // Slate gray
val TextTertiaryLight = Color(0xFFC7C7CC)    // Low-contrast gray

// Signal Accents (Single Chromatic Focus)
val SignalGreen = Color(0xFF10A37F)          // OpenAI signature emerald
val SignalGreenPulse = Color(0xFF00FF66)     // High-visibility live dot accent
val SignalOrange = Color(0xFFFF5700)         // Vibrant energetic orange accent
val SignalError = Color(0xFFFF453A)          // Stark warning red

val DatumDarkColorScheme = darkColorScheme(
    background = SurfaceDark,
    surface = CardSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextPrimaryDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    outline = CardBorderDark,
    primary = TextPrimaryDark,
    secondary = TextSecondaryDark,
    tertiary = TextTertiaryDark,
    primaryContainer = SignalOrange
)

val DatumLightColorScheme = lightColorScheme(
    background = SurfaceLight,
    surface = CardSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextPrimaryLight,
    secondaryContainer = SurfaceVariantLight,
    onSecondaryContainer = TextPrimaryLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    outline = CardBorderLight,
    primary = TextPrimaryLight,
    secondary = TextSecondaryLight,
    tertiary = TextTertiaryLight,
    primaryContainer = SignalOrange
)

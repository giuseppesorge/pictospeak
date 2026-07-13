@file:Suppress("MagicNumber") // this file IS the color palette; the hex values are the point

package io.github.giuseppesorge.pictospeak.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * PictoSpeak brand palette — a calm teal, chosen to read as a distinct, trustworthy identity
 * and to sit quietly behind the bright Fitzgerald pictogram colors (which are the content and
 * never change). Hand-authored light + dark Material 3 schemes: predictable, high-contrast,
 * and NOT wallpaper-driven — a communication aid must look the same for every user.
 *
 * The pictogram cell colors live in FitzgeraldSlot (ui/board) and are deliberately
 * theme-independent; their labels use OnPictogram so dark text stays readable on those
 * pastels in both light and dark mode.
 */

// Dark, near-black text for labels drawn on the light Fitzgerald pastels (both themes).
val OnPictogram = Color(0xFF1A1C1C)

val LightColors =
    lightColorScheme(
        primary = Color(0xFF00696E),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF9CF0F6),
        onPrimaryContainer = Color(0xFF002022),
        secondary = Color(0xFF4A6365),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E9),
        onSecondaryContainer = Color(0xFF051F21),
        tertiary = Color(0xFF4B607C),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFD3E4FF),
        onTertiaryContainer = Color(0xFF041C35),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = Color(0xFFF5FAFA),
        onBackground = Color(0xFF171D1D),
        surface = Color(0xFFF5FAFA),
        onSurface = Color(0xFF171D1D),
        surfaceVariant = Color(0xFFDAE4E4),
        onSurfaceVariant = Color(0xFF3F4948),
        surfaceContainerHigh = Color(0xFFE7EEEE),
        surfaceContainerHighest = Color(0xFFE1E9E9),
        outline = Color(0xFF6F7979),
        outlineVariant = Color(0xFFBEC8C8),
    )

val DarkColors =
    darkColorScheme(
        primary = Color(0xFF80D4DA),
        onPrimary = Color(0xFF00363A),
        primaryContainer = Color(0xFF004F53),
        onPrimaryContainer = Color(0xFF9CF0F6),
        secondary = Color(0xFFB0CCCD),
        onSecondary = Color(0xFF1B3436),
        secondaryContainer = Color(0xFF324B4C),
        onSecondaryContainer = Color(0xFFCCE8E9),
        tertiary = Color(0xFFB4C8E9),
        onTertiary = Color(0xFF1C314C),
        tertiaryContainer = Color(0xFF344863),
        onTertiaryContainer = Color(0xFFD3E4FF),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = Color(0xFF0E1415),
        onBackground = Color(0xFFDEE3E3),
        surface = Color(0xFF0E1415),
        onSurface = Color(0xFFDEE3E3),
        surfaceVariant = Color(0xFF3F4948),
        onSurfaceVariant = Color(0xFFBEC8C8),
        surfaceContainerHigh = Color(0xFF252B2B),
        surfaceContainerHighest = Color(0xFF303636),
        outline = Color(0xFF899392),
        outlineVariant = Color(0xFF3F4948),
    )

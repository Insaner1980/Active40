package com.finnvek.active40.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material-komponenttien oletusvärit johdetaan tokeneista. Tausta on liukuväri (GreenBackground),
// joten background/surface ovat läpinäkyviä, jotta liukuväri näkyy Scaffoldin läpi.
private val Active40ColorScheme =
    darkColorScheme(
        primary = Active40Colors.surfaceWhite,
        onPrimary = Active40Colors.onSurface,
        secondary = Active40Colors.accentStrength,
        onSecondary = Active40Colors.onSurface,
        tertiary = Active40Colors.accentRun,
        onTertiary = Active40Colors.onSurface,
        background = Color.Transparent,
        onBackground = Active40Colors.onBackground,
        surface = Color.Transparent,
        onSurface = Active40Colors.onBackground,
        onSurfaceVariant = Active40Colors.onBackgroundMuted,
        surfaceContainer = Active40Colors.inkDark,
        outline = Active40Colors.outlineGlass,
        outlineVariant = Active40Colors.outlineGlass,
    )

@Composable
fun Active40Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Active40ColorScheme,
        typography = Active40Typography,
        shapes = Active40MaterialShapes,
        content = content,
    )
}

// Vaaleille pinnoille avautuvat Material-komponentit (esim. päivämäärävalitsin), joiden värejä ei voi antaa suoraan.
private val LightSurfaceColorScheme =
    lightColorScheme(
        primary = Active40Colors.inkDark,
        onPrimary = Active40Colors.onBackground,
        secondaryContainer = Active40Colors.surfaceWhite,
        onSecondaryContainer = Active40Colors.onSurface,
        surface = Active40Colors.surfaceLight,
        onSurface = Active40Colors.onSurface,
        onSurfaceVariant = Active40Colors.onSurfaceMuted,
        surfaceContainerHigh = Active40Colors.surfaceLight,
        surfaceContainerHighest = Active40Colors.surfaceWhite,
        outline = Active40Colors.onSurfaceMuted,
        outlineVariant = Active40Colors.onSurfaceMuted,
    )

@Composable
fun LightSurfaceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightSurfaceColorScheme,
        typography = Active40Typography,
        shapes = Active40MaterialShapes,
        content = content,
    )
}

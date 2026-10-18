package com.caos.dominocaos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Esquema unico oscuro, sin colores dinamicos.
private val DominoColorScheme = darkColorScheme(
    primary = Lime,
    onPrimary = Ink900,
    secondary = TextLow,
    onSecondary = Ink900,
    background = Ink900,
    onBackground = TextHigh,
    surface = Ink800,
    onSurface = TextHigh,
    surfaceVariant = Ink700,
    onSurfaceVariant = TextLow,
    outline = Ink600,
    error = Coral,
    onError = Ink900
)

@Composable
fun DominoCaosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DominoColorScheme, typography = Typography, content = content)
}
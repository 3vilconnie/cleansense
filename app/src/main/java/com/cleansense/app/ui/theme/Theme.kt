package com.cleansense.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CleanSenseColors = darkColorScheme(
    primary = CsMint,
    onPrimary = CsOnMint,
    background = CsBackground,
    onBackground = CsTextPrimary,
    surface = CsBackground,
    onSurface = CsTextPrimary,
    surfaceVariant = CsSurface,
    onSurfaceVariant = CsTextSecondary,
    outline = CsOutline,
    error = CsError,
)

@Composable
fun CleanSenseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CleanSenseColors, content = content)
}

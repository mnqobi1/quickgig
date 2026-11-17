package com.quickgig.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = QuickGigPurple,
    onPrimary = QuickGigSurface,
    secondary = QuickGigPurpleBright,
    background = QuickGigBackground,
    surface = QuickGigSurface,
    surfaceVariant = QuickGigLavenderTint,
    onBackground = QuickGigNavy,
    onSurface = QuickGigNavy,
    outline = QuickGigBorder,
    error = QuickGigError,
)

private val DarkColors = darkColorScheme(
    primary = QuickGigPurpleBright,
    onPrimary = QuickGigNavy,
    secondary = QuickGigPurple,
    background = Color(0xFF0F1220),
    surface = Color(0xFF171A2C),
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun QuickGigTheme(useDarkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (useDarkTheme) DarkColors else LightColors,
        typography = QuickGigTypography,
        content = content,
    )
}

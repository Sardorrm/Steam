package com.example.donttrustthehouse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = CrimsonDark,
    onPrimaryContainer = Color.White,
    secondary = GhostlyCyan,
    onSecondary = Color.Black,
    tertiary = StealthGreen,
    background = HorrorDarkBackground,
    onBackground = TextPrimary,
    surface = HorrorSurface,
    onSurface = TextPrimary,
    surfaceVariant = HorrorSurfaceVariant,
    onSurfaceVariant = TextSecondary
)

@Composable
fun DontTrustTheHouseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

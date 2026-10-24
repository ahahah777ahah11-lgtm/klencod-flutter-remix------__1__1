package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val AppColorScheme = lightColorScheme(
    primary = PrimaryNavy,
    onPrimary = CardWhite,
    primaryContainer = SecondaryBlue,
    onPrimaryContainer = CardWhite,
    secondary = SecondaryBlue,
    onSecondary = CardWhite,
    background = BackgroundIce,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundIce,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
    error = RedDebt,
    onError = CardWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force RTL layout direction across the entire app
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = AppColorScheme,
            typography = Typography,
            content = content
        )
    }
}

package com.example.financetracker.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkFinanceGreen,
    secondary = DarkFinanceBlue,
    tertiary = DarkFinanceRed,

    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,

    primaryContainer = Color(0xFF003300),
    onPrimaryContainer = Color(0xFFC8E6C9),

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCAC4D0),

    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black
)

@Composable
fun FinanceTrackerTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
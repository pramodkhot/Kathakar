package com.kathakar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OrangeLight = Color(0xFFE05C00)
private val OrangeDark  = Color(0xFFFF8A40)

private val LightColors = lightColorScheme(
    primary          = OrangeLight,
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFFFDBCA),
    secondary        = Color(0xFF995300),
    onSecondary      = Color.White,
)

private val DarkColors = darkColorScheme(
    primary          = OrangeDark,
    onPrimary        = Color(0xFF4A1500),
    primaryContainer = Color(0xFF6B2D00),
    secondary        = Color(0xFFFFB783),
)

@Composable
fun KathakarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}

package com.kathakar.app.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val Orange = Color(0xFFE05C00)
private val LightColors = lightColorScheme(primary = Orange, onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCA), onPrimaryContainer = Color(0xFF3A0D00),
    secondary = Color(0xFF995300), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCBD), onSecondaryContainer = Color(0xFF301700))
private val DarkColors = darkColorScheme(primary = Color(0xFFFF8A40), onPrimary = Color(0xFF4A1500),
    primaryContainer = Color(0xFF6B2D00), onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = Color(0xFFFFB783), onSecondary = Color(0xFF4A2500))
@Composable
fun KathakarTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}

package com.voiceime.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val VoiceIMEDarkColors = darkColorScheme(
    primary = Color(0xFFFF5C35),
    secondary = Color(0xFF00D4AA),
    background = Color(0xFF111111),
    surface = Color(0xFF1C1C1C),
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFCCCCCC),
    error = Color(0xFFFF4444),
    onPrimary = Color.White,
    onSecondary = Color.Black
)

val VoiceIMELightColors = lightColorScheme(
    primary = Color(0xFFFF5C35),
    secondary = Color(0xFF00D4AA),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1C1C1C),
    onSurface = Color(0xFF444444),
    error = Color(0xFFD32F2F),
    onPrimary = Color.White,
    onSecondary = Color.Black
)

@Composable
fun VoiceIMETheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VoiceIMEDarkColors else VoiceIMELightColors


    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

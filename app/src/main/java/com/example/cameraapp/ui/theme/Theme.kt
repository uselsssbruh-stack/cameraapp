package com.example.cameraapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UBCameraDarkColorScheme = darkColorScheme(
    primary          = AccentGold,
    onPrimary        = Color.Black,
    primaryContainer = AccentGoldLight,
    secondary        = AccentGoldLight,
    onSecondary      = Color.Black,
    tertiary         = AccentRed,
    background       = DarkBackground,
    onBackground     = WhiteAlpha90,
    surface          = DarkSurface,
    onSurface        = WhiteAlpha90,
    surfaceVariant   = DarkCard,
    onSurfaceVariant = WhiteAlpha60,
    error            = AccentRed,
    onError          = Color.White,
)

@Composable
fun CameraAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UBCameraDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}


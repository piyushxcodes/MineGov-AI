package com.minegov.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,

    secondary = LightGray,
    onSecondary = Black,

    background = Black,
    onBackground = White,

    surface = DarkBackground,
    onSurface = White,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightGray
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,

    secondary = Gray,
    onSecondary = White,

    background = White,
    onBackground = Black,

    surface = LightBackground,
    onSurface = Black,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Black
)

@Composable
fun MineGovAITheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        },
        typography = Typography(),
        content = content
    )
}
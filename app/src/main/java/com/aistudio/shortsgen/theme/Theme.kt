package com.aistudio.shortsgen.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Sky400,
    secondary = Violet400,
    tertiary = Emerald400,
    background = Slate900,
    surface = Slate800,
    onPrimary = Slate950,
    onSecondary = Slate950,
    onTertiary = Slate950,
    onBackground = Slate50,
    onSurface = Slate100,
    error = Rose400
)

@Composable
fun ShortsGenAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

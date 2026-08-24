package com.sonora.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SonoraPaperBeige = Color(0xFFF5F2EA)
val SonoraPaperCard = Color(0xFFEAE5DA)
val SonoraPaperBorder = Color(0xFFDED8CD)
val SonoraPaperTextPrimary = Color(0xFF121212)
val SonoraPaperTextSecondary = Color(0xFF75726B)

val SonoraObsidianDark = Color(0xFF0F0E0D)
val SonoraObsidianCard = Color(0xFF1A1917)
val SonoraObsidianBorder = Color(0xFF2A2824)
val SonoraObsidianTextPrimary = Color(0xFFF5F2EA)
val SonoraObsidianTextSecondary = Color(0xFF8A857B)

val SonoraGold = Color(0xFFC5A059)
val SonoraAccentRed = Color(0xFFEF4444)

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    surface = SonoraObsidianCard,
    onSurface = SonoraObsidianTextPrimary,
    background = SonoraObsidianDark,
    onBackground = SonoraObsidianTextPrimary,
    outline = SonoraObsidianBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    surface = SonoraPaperCard,
    onSurface = SonoraPaperTextPrimary,
    background = SonoraPaperBeige,
    onBackground = SonoraPaperTextPrimary,
    outline = SonoraPaperBorder
)

@Composable
fun SonoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SonoraTypography,
        content = content
    )
}

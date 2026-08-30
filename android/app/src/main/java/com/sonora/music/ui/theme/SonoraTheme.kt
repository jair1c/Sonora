package com.sonora.music.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

// Classic Paper Light
val SonoraPaperBeige = Color(0xFFF5F2EA)
val SonoraPaperCard = Color(0xFFEAE5DA)
val SonoraPaperSubCard = Color(0xFFDFD9CE)
val SonoraPaperBorder = Color(0xFFDED8CD)
val SonoraPaperTextPrimary = Color(0xFF121212)
val SonoraPaperTextSecondary = Color(0xFF75726B)

// Classic Obsidian Dark
val SonoraObsidianDark = Color(0xFF0F0E0D)
val SonoraObsidianCard = Color(0xFF1A1917)
val SonoraObsidianSubCard = Color(0xFF24221F)
val SonoraObsidianBorder = Color(0xFF2A2824)
val SonoraObsidianTextPrimary = Color(0xFFF5F2EA)
val SonoraObsidianTextSecondary = Color(0xFF8A857B)

// Apple Liquid Glass - Dark (Midnight Smoke Glass)
val SonoraGlassDarkBg = Color(0xFF0A0C10)
val SonoraGlassDarkCard = Color(0x24FFFFFF)      // Frosted translucent smoke glass (~14% alpha)
val SonoraGlassDarkSubCard = Color(0x15FFFFFF)   // Subtle glass layer (~8% alpha)
val SonoraGlassDarkBorder = Color(0x38FFFFFF)    // Specular frosted glass rim (~22% alpha)
val SonoraGlassDarkTextPrimary = Color(0xFFF8FAFC)
val SonoraGlassDarkTextSecondary = Color(0xFF94A3B8)
val SonoraGlassDarkPillBg = Color(0xEEFFFFFF)
val SonoraGlassDarkPillText = Color(0xFF0A0C10)

// Apple Liquid Glass - Light (Crystal Ice Glass)
val SonoraGlassLightBg = Color(0xFFECEFF4)
val SonoraGlassLightCard = Color(0x80FFFFFF)     // Frosted crystalline ice glass (~50% alpha)
val SonoraGlassLightSubCard = Color(0x55FFFFFF)  // Semi-opaque glass layer (~33% alpha)
val SonoraGlassLightBorder = Color(0xB3FFFFFF)   // Glistening crystal rim (~70% alpha)
val SonoraGlassLightTextPrimary = Color(0xFF0F172A)
val SonoraGlassLightTextSecondary = Color(0xFF475569)
val SonoraGlassLightPillBg = Color(0xFF0F172A)
val SonoraGlassLightPillText = Color(0xFFFFFFFF)

val SonoraGold = Color(0xFFC5A059)
val SonoraAccentRed = Color(0xFFEF4444)

data class SonoraThemeColors(
    val bg: Color,
    val cardBg: Color,
    val subCardBg: Color,
    val borderCol: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val activePillBg: Color,
    val activePillText: Color,
    val isDark: Boolean,
    val isGlass: Boolean,
    val glassBorderBrush: Brush = SolidColor(borderCol),
    val glassSpecularHighlight: Color = Color.Transparent
)

val LocalSonoraColors = staticCompositionLocalOf {
    SonoraThemeColors(
        bg = SonoraObsidianDark,
        cardBg = SonoraObsidianCard,
        subCardBg = SonoraObsidianSubCard,
        borderCol = SonoraObsidianBorder,
        textPrimary = SonoraObsidianTextPrimary,
        textSecondary = SonoraObsidianTextSecondary,
        activePillBg = Color.White,
        activePillText = Color.Black,
        isDark = true,
        isGlass = false
    )
}

fun getSonoraThemeColors(isDark: Boolean, isGlass: Boolean): SonoraThemeColors {
    return if (isGlass) {
        if (isDark) {
            SonoraThemeColors(
                bg = SonoraGlassDarkBg,
                cardBg = SonoraGlassDarkCard,
                subCardBg = SonoraGlassDarkSubCard,
                borderCol = SonoraGlassDarkBorder,
                textPrimary = SonoraGlassDarkTextPrimary,
                textSecondary = SonoraGlassDarkTextSecondary,
                activePillBg = SonoraGlassDarkPillBg,
                activePillText = SonoraGlassDarkPillText,
                isDark = true,
                isGlass = true,
                glassBorderBrush = Brush.linearGradient(
                    listOf(
                        Color(0x60FFFFFF),
                        Color(0x18FFFFFF),
                        Color(0x40FFFFFF)
                    )
                ),
                glassSpecularHighlight = Color(0x28FFFFFF)
            )
        } else {
            SonoraThemeColors(
                bg = SonoraGlassLightBg,
                cardBg = SonoraGlassLightCard,
                subCardBg = SonoraGlassLightSubCard,
                borderCol = SonoraGlassLightBorder,
                textPrimary = SonoraGlassLightTextPrimary,
                textSecondary = SonoraGlassLightTextSecondary,
                activePillBg = SonoraGlassLightPillBg,
                activePillText = SonoraGlassLightPillText,
                isDark = false,
                isGlass = true,
                glassBorderBrush = Brush.linearGradient(
                    listOf(
                        Color(0xE6FFFFFF),
                        Color(0x80FFFFFF),
                        Color(0xCCFFFFFF)
                    )
                ),
                glassSpecularHighlight = Color(0x50FFFFFF)
            )
        }
    } else {
        if (isDark) {
            SonoraThemeColors(
                bg = SonoraObsidianDark,
                cardBg = SonoraObsidianCard,
                subCardBg = SonoraObsidianSubCard,
                borderCol = SonoraObsidianBorder,
                textPrimary = SonoraObsidianTextPrimary,
                textSecondary = SonoraObsidianTextSecondary,
                activePillBg = Color.White,
                activePillText = Color.Black,
                isDark = true,
                isGlass = false
            )
        } else {
            SonoraThemeColors(
                bg = SonoraPaperBeige,
                cardBg = SonoraPaperCard,
                subCardBg = SonoraPaperSubCard,
                borderCol = SonoraPaperBorder,
                textPrimary = SonoraPaperTextPrimary,
                textSecondary = SonoraPaperTextSecondary,
                activePillBg = Color(0xFF121212),
                activePillText = Color.White,
                isDark = false,
                isGlass = false
            )
        }
    }
}

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
    isGlass: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeColors = getSonoraThemeColors(isDark = darkTheme, isGlass = isGlass)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SonoraTypography
    ) {
        CompositionLocalProvider(
            LocalSonoraColors provides themeColors,
            LocalTextStyle provides TextStyle(
                fontFamily = PlusJakartaSansFamily,
                color = themeColors.textPrimary
            )
        ) {
            content()
        }
    }
}

@Composable
fun LiquidGlassBackdrop(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LiquidGlassTransition")
    val animOffset1 = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animOffset1"
    )
    val animOffset2 = infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animOffset2"
    )

    val baseBg = if (isDark) SonoraGlassDarkBg else SonoraGlassLightBg
    val orb1Color = if (isDark) Color(0x352563EB) else Color(0x4060A5FA) // Cobalt Blue / Cyan
    val orb2Color = if (isDark) Color(0x2D7C3AED) else Color(0x35A78BFA) // Violet Indigo
    val orb3Color = if (isDark) Color(0x22D4AF37) else Color(0x30FBBF24) // Liquid Amber Gold

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
        ) {
            val w = size.width
            val h = size.height

            // Orb 1 (Top right / Center)
            drawCircle(
                color = orb1Color,
                radius = w * 0.6f,
                center = Offset(
                    x = w * (0.65f + 0.15f * animOffset1.value),
                    y = h * (0.2f + 0.12f * animOffset2.value)
                )
            )

            // Orb 2 (Bottom left / Middle)
            drawCircle(
                color = orb2Color,
                radius = w * 0.7f,
                center = Offset(
                    x = w * (0.25f - 0.15f * animOffset2.value),
                    y = h * (0.62f - 0.15f * animOffset1.value)
                )
            )

            // Orb 3 (Bottom right / Accent)
            drawCircle(
                color = orb3Color,
                radius = w * 0.5f,
                center = Offset(
                    x = w * (0.8f - 0.2f * animOffset1.value),
                    y = h * (0.85f - 0.1f * animOffset2.value)
                )
            )
        }
    }
}

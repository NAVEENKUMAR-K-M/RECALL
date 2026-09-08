package com.aigallery.app.core.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class GlassColorPalette(
    val surface: Color,
    val surfaceSubtle: Color,
    val surfaceHeavy: Color,
    val border: Color,
    val borderSubtle: Color,
    val innerGlow: Color,
    val shadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val specularBrush: Brush,
    val isDark: Boolean
)

val LocalGlassColors = staticCompositionLocalOf {
    GlassColorPalette(
        surface = ColorTokens.DarkGlassSurface,
        surfaceSubtle = ColorTokens.DarkGlassSurfaceSubtle,
        surfaceHeavy = ColorTokens.DarkGlassSurfaceHeavy,
        border = ColorTokens.DarkGlassBorder,
        borderSubtle = ColorTokens.DarkGlassBorderSubtle,
        innerGlow = ColorTokens.DarkGlassInnerGlow,
        shadow = ColorTokens.DarkGlassShadow,
        textPrimary = ColorTokens.TextPrimaryDark,
        textSecondary = ColorTokens.TextSecondaryDark,
        textTertiary = ColorTokens.TextTertiaryDark,
        specularBrush = ColorTokens.DarkGlassSpecularBrush,
        isDark = true
    )
}

private val DarkGlassPalette = GlassColorPalette(
    surface = ColorTokens.DarkGlassSurface,
    surfaceSubtle = ColorTokens.DarkGlassSurfaceSubtle,
    surfaceHeavy = ColorTokens.DarkGlassSurfaceHeavy,
    border = ColorTokens.DarkGlassBorder,
    borderSubtle = ColorTokens.DarkGlassBorderSubtle,
    innerGlow = ColorTokens.DarkGlassInnerGlow,
    shadow = ColorTokens.DarkGlassShadow,
    textPrimary = ColorTokens.TextPrimaryDark,
    textSecondary = ColorTokens.TextSecondaryDark,
    textTertiary = ColorTokens.TextTertiaryDark,
    specularBrush = ColorTokens.DarkGlassSpecularBrush,
    isDark = true
)

private val LightGlassPalette = GlassColorPalette(
    surface = ColorTokens.LightGlassSurface,
    surfaceSubtle = ColorTokens.LightGlassSurfaceSubtle,
    surfaceHeavy = ColorTokens.LightGlassSurfaceHeavy,
    border = ColorTokens.LightGlassBorder,
    borderSubtle = ColorTokens.LightGlassBorderSubtle,
    innerGlow = ColorTokens.LightGlassInnerGlow,
    shadow = ColorTokens.LightGlassShadow,
    textPrimary = ColorTokens.TextPrimaryLight,
    textSecondary = ColorTokens.TextSecondaryLight,
    textTertiary = ColorTokens.TextTertiaryLight,
    specularBrush = ColorTokens.LightGlassSpecularBrush,
    isDark = false
)

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = ColorTokens.AccentElectric,
    onSecondary = Color.Black,
    background = ColorTokens.DarkBackground,
    onBackground = ColorTokens.TextPrimaryDark,
    surface = ColorTokens.DarkSurface,
    onSurface = ColorTokens.TextPrimaryDark,
    surfaceVariant = ColorTokens.DarkSurfaceElevated,
    onSurfaceVariant = ColorTokens.TextSecondaryDark,
    outline = ColorTokens.DarkGlassBorderSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    secondary = Color(0xFF007A87),
    onSecondary = Color.White,
    background = ColorTokens.LightBackground,
    onBackground = ColorTokens.TextPrimaryLight,
    surface = ColorTokens.LightSurface,
    onSurface = ColorTokens.TextPrimaryLight,
    surfaceVariant = ColorTokens.LightSurfaceElevated,
    onSurfaceVariant = ColorTokens.TextSecondaryLight,
    outline = ColorTokens.LightGlassBorderSubtle
)

@Composable
fun AIGalleryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val glassColors = if (darkTheme) DarkGlassPalette else LightGlassPalette

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalGlassColors provides glassColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TypographyTokens,
            content = content
        )
    }
}

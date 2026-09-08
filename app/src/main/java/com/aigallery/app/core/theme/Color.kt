package com.aigallery.app.core.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * ColorTokens for AI Gallery.
 * High-end flagship aesthetic: Deep AMOLED charcoal base, physical liquid glass overlays,
 * subtle specular highlights, and restrained neutral accents.
 */
object ColorTokens {
    // Dark Theme Palette (Flagship Showcase Mode)
    val DarkBackground = Color(0xFF08080A)
    val DarkSurface = Color(0xFF101014)
    val DarkSurfaceElevated = Color(0xFF16161C)
    val DarkSurfaceHighlight = Color(0xFF22222C)

    // Glass Overlays (Dark)
    val DarkGlassSurface = Color(0x28181820)
    val DarkGlassSurfaceSubtle = Color(0x18FFFFFF)
    val DarkGlassSurfaceHeavy = Color(0x55121218)
    val DarkGlassBorder = Color(0x35FFFFFF)
    val DarkGlassBorderSubtle = Color(0x1AFFFFFF)
    val DarkGlassInnerGlow = Color(0x0DFFFFFF)
    val DarkGlassShadow = Color(0x80000000)

    // Light Theme Palette
    val LightBackground = Color(0xFFF7F7FA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceElevated = Color(0xFFF0F0F4)
    val LightSurfaceHighlight = Color(0xFFE5E5EB)

    // Glass Overlays (Light)
    val LightGlassSurface = Color(0x66FFFFFF)
    val LightGlassSurfaceSubtle = Color(0x40FFFFFF)
    val LightGlassSurfaceHeavy = Color(0x99FFFFFF)
    val LightGlassBorder = Color(0x40000000)
    val LightGlassBorderSubtle = Color(0x18000000)
    val LightGlassInnerGlow = Color(0x20FFFFFF)
    val LightGlassShadow = Color(0x1A000000)

    // Typography
    val TextPrimaryDark = Color(0xFFF6F6F8)
    val TextSecondaryDark = Color(0xFF9E9EA6)
    val TextTertiaryDark = Color(0xFF63636B)

    val TextPrimaryLight = Color(0xFF111114)
    val TextSecondaryLight = Color(0xFF6B6B75)
    val TextTertiaryLight = Color(0xFF9999A3)

    // Accent Tones (Minimal, Flagship Precision)
    val AccentElectric = Color(0xFF00E5FF)
    val AccentHeart = Color(0xFFFF3B5C)
    val AccentGlassGlow = Color(0x40FFFFFF)

    // Specular Edge Gradients for Beveled Liquid Glass
    val DarkGlassSpecularBrush = Brush.linearGradient(
        listOf(
            Color(0x45FFFFFF),
            Color(0x12FFFFFF),
            Color(0x05FFFFFF),
            Color(0x20FFFFFF)
        )
    )

    val LightGlassSpecularBrush = Brush.linearGradient(
        listOf(
            Color(0x80FFFFFF),
            Color(0x20FFFFFF),
            Color(0x10000000),
            Color(0x30FFFFFF)
        )
    )

    // Floating Tab Active Indicator Glow Brush
    val ActiveTabGlowBrush = Brush.horizontalGradient(
        listOf(
            Color(0x3300E5FF),
            Color(0x4DFFFFFF),
            Color(0x3300E5FF)
        )
    )
}

val AccentCyan = Color(0xFF00E5FF)
val AccentPurple = Color(0xFF9C27B0)
val DarkBackground = Color(0xFF08080A)
val GlassSurfaceDark = Color(0x35101018)
val GlassBorderLight = Color(0x28FFFFFF)

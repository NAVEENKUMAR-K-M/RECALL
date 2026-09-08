package com.aigallery.app.core.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized GlassTokens defining blur, borders, shapes, and spacing
 * for the Liquid Glass design language.
 */
object GlassTokens {
    // Blur Radius
    val BlurSubtle: Dp = 10.dp
    val BlurStandard: Dp = 18.dp
    val BlurDeep: Dp = 28.dp

    // Border Strokes
    val BorderSubtle: Dp = 0.75.dp
    val BorderStandard: Dp = 1.dp
    val BorderEmphasized: Dp = 1.25.dp

    // Elevation & Depth
    val ElevationNone: Dp = 0.dp
    val ElevationSubtle: Dp = 6.dp
    val ElevationFloating: Dp = 16.dp
    val ElevationSheet: Dp = 24.dp

    // Opacities
    const val AlphaContainer = 0.72f
    const val AlphaContainerSubtle = 0.45f
    const val AlphaFloatingBar = 0.82f
    const val AlphaScrim = 0.65f
}

object SpacingTokens {
    val Tiny: Dp = 2.dp
    val ExtraSmall: Dp = 4.dp
    val Small: Dp = 8.dp
    val Medium: Dp = 12.dp
    val Large: Dp = 16.dp
    val ExtraLarge: Dp = 20.dp
    val Section: Dp = 24.dp
    val DoubleSection: Dp = 32.dp
    val FloatingBarBottomOffset: Dp = 20.dp
}

object ShapeTokens {
    val ImageCorner = RoundedCornerShape(12.dp)
    val Chip = RoundedCornerShape(14.dp)
    val Card = RoundedCornerShape(20.dp)
    val Pill = RoundedCornerShape(32.dp)
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val Circular = CircleShape
}

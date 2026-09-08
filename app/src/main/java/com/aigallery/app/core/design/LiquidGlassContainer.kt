package com.aigallery.app.core.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aigallery.app.core.theme.GlassTokens

/**
 * Reusable LiquidGlassContainer providing physical frosted translucent glass.
 */
@Composable
fun LiquidGlassContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color? = null,
    borderBrush: Brush? = null,
    borderWidth: Dp = GlassTokens.BorderStandard,
    elevation: Dp = GlassTokens.ElevationFloating,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlass(
            shape = shape,
            backgroundColor = backgroundColor,
            borderBrush = borderBrush,
            borderWidth = borderWidth,
            elevation = elevation
        ),
        content = content
    )
}

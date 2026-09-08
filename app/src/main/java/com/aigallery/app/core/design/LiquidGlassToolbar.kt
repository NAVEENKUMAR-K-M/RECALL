package com.aigallery.app.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aigallery.app.core.theme.GlassTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.ShapeTokens

@Composable
fun LiquidGlassToolbar(
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    height: Dp = 56.dp,
    content: @Composable RowScope.() -> Unit
) {
    val glassColors = LocalGlassColors.current

    LiquidGlassContainer(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = ShapeTokens.Pill,
        backgroundColor = backgroundColor ?: glassColors.surfaceHeavy,
        elevation = GlassTokens.ElevationFloating
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            content = content
        )
    }
}

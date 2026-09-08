package com.aigallery.app.core.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aigallery.app.core.theme.GlassTokens
import com.aigallery.app.core.theme.LocalGlassColors

/**
 * Modifier that applies the signature Liquid Glass styling:
 * - Frosted backdrop translucency
 * - Specular beveled border highlight
 * - Diffuse ambient shadow
 * - Top specular highlight sheen
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color? = null,
    borderBrush: Brush? = null,
    borderWidth: Dp = GlassTokens.BorderStandard,
    elevation: Dp = GlassTokens.ElevationFloating
): Modifier {
    val glassColors = LocalGlassColors.current
    val bg = backgroundColor ?: glassColors.surface
    val border = borderBrush ?: glassColors.specularBrush

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = glassColors.shadow,
            spotColor = glassColors.shadow
        )
        .clip(shape)
        .background(bg, shape)
        .border(borderWidth, border, shape)
        .drawWithContent {
            drawContent()
            // Top specular shine highlight (simulates physical light reflection on curved glass)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x28FFFFFF),
                        Color(0x08FFFFFF),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = size.height * 0.45f
                )
            )
        }
}

/**
 * Interactive press scale effect for glass buttons and controls.
 */
@Composable
fun Modifier.glassPressEffect(
    pressedScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 600f),
        label = "glassPressScale"
    )

    val modifier = this.scale(scale)
    return if (onClick != null) {
        modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        modifier
    }
}

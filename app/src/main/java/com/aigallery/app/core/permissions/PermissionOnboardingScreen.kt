package com.aigallery.app.core.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigallery.app.core.design.LiquidGlassButton
import com.aigallery.app.core.design.LiquidGlassContainer
import com.aigallery.app.core.theme.ColorTokens
import com.aigallery.app.core.theme.GlassTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.ShapeTokens
import com.aigallery.app.core.theme.SpacingTokens

@Composable
fun PermissionOnboardingScreen(
    onContinueClick: () -> Unit
) {
    val glassColors = LocalGlassColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(glassColors.isDark.let { if (it) ColorTokens.DarkBackground else ColorTokens.LightBackground })
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow orbs
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x3300E5FF),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingTokens.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LiquidGlassContainer(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeTokens.Card,
                backgroundColor = glassColors.surfaceHeavy,
                elevation = GlassTokens.ElevationFloating
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Shield Icon with Glass Glow
                    LiquidGlassContainer(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        backgroundColor = Color(0x20FFFFFF),
                        elevation = GlassTokens.ElevationSubtle
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = "Privacy Shield",
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center),
                            tint = ColorTokens.AccentElectric
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "YOUR MEMORIES STAY YOURS",
                        color = glassColors.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "iQOO Gallery needs access to your photos and videos so it can display and organize your personal gallery.\n\nEverything stays on your device.",
                        color = glassColors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Privacy Pill Highlights
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PrivacyBadge(icon = Icons.Rounded.WifiOff, label = "Offline")
                        PrivacyBadge(icon = Icons.Rounded.Lock, label = "On-Device")
                        PrivacyBadge(icon = Icons.Rounded.VisibilityOff, label = "Zero Tracking")
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    LiquidGlassButton(
                        onClick = onContinueClick,
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0x35FFFFFF)
                    ) {
                        Text(
                            text = "Continue",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    val glassColors = LocalGlassColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = ColorTokens.AccentElectric
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = glassColors.textTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

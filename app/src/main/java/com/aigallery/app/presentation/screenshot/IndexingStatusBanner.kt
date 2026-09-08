package com.aigallery.app.presentation.screenshot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigallery.app.core.theme.AccentCyan
import com.aigallery.app.core.theme.AccentPurple
import com.aigallery.app.core.theme.GlassBorderLight
import com.aigallery.app.core.theme.GlassSurfaceDark
import com.aigallery.app.domain.repository.ScreenshotAIStats

@Composable
fun IndexingStatusBanner(
    stats: ScreenshotAIStats,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVisible = stats.totalCount > 0
    val isProcessing = stats.pendingCount > 0

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alphaGlow by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GlassSurfaceDark)
                .border(
                    width = 1.dp,
                    brush = if (isProcessing) {
                        Brush.horizontalGradient(
                            listOf(
                                AccentCyan.copy(alpha = alphaGlow),
                                AccentPurple.copy(alpha = alphaGlow * 0.7f),
                                Color.White.copy(alpha = 0.15f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                GlassBorderLight,
                                Color.White.copy(alpha = 0.05f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isProcessing) {
                            Icon(
                                imageVector = Icons.Rounded.HourglassTop,
                                contentDescription = "Indexing",
                                tint = AccentCyan,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer {
                                        scaleX = 0.85f + (alphaGlow * 0.25f)
                                        scaleY = 0.85f + (alphaGlow * 0.25f)
                                    }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = if (isProcessing) {
                                "Understanding screenshots"
                            } else {
                                "${stats.completedCount} screenshots understood"
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (isProcessing) {
                        Text(
                            text = "${stats.completedCount} / ${stats.totalCount}",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "On-Device",
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Smooth Progress Bar when actively indexing
                if (isProcessing && stats.totalCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val progress = (stats.completedCount.toFloat() / stats.totalCount.toFloat()).coerceIn(0.05f, 1.0f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(AccentCyan, AccentPurple)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

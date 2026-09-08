package com.aigallery.app.core.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aigallery.app.core.theme.GlassTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.ShapeTokens
import com.aigallery.app.core.theme.SpacingTokens

enum class NavigationTab(
    val title: String,
    val icon: ImageVector
) {
    HOME("Photos", Icons.Rounded.Photo),
    ALBUMS("Albums", Icons.Rounded.FolderCopy),
    AI("Organize", Icons.Rounded.FolderSpecial),
    SEARCH("Search", Icons.Rounded.Search),
    FAVORITES("Favorites", Icons.Rounded.Favorite)
}

@Composable
fun LiquidGlassBottomBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                horizontal = SpacingTokens.Large,
                vertical = SpacingTokens.Medium
            ),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassContainer(
            modifier = Modifier.height(64.dp),
            shape = ShapeTokens.Pill,
            backgroundColor = glassColors.surfaceHeavy,
            elevation = GlassTokens.ElevationFloating
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab

                    val targetBackground = if (isSelected) {
                        if (glassColors.isDark) Color(0x33FFFFFF) else Color(0x2B000000)
                    } else {
                        Color.Transparent
                    }

                    val animatedBg by animateColorAsState(
                        targetValue = targetBackground,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "tabBg"
                    )

                    val targetTextColor = if (isSelected) {
                        glassColors.textPrimary
                    } else {
                        glassColors.textSecondary
                    }

                    val animatedTextColor by animateColorAsState(
                        targetValue = targetTextColor,
                        label = "tabTextColor"
                    )

                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.05f else 1.0f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
                        label = "tabScale"
                    )

                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .clip(ShapeTokens.Pill)
                            .background(animatedBg)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onTabSelected(tab) }
                            )
                            .defaultMinSize(minWidth = 52.dp, minHeight = 48.dp)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp),
                                tint = animatedTextColor
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    color = animatedTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

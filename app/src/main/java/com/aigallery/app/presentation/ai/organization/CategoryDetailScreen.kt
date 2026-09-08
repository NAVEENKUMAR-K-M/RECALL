package com.aigallery.app.presentation.ai.organization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigallery.app.core.design.LiquidGlassToolbar
import com.aigallery.app.core.theme.AccentCyan
import com.aigallery.app.core.theme.AccentPurple
import com.aigallery.app.core.theme.DarkBackground
import com.aigallery.app.core.theme.GlassBorderLight
import com.aigallery.app.core.theme.GlassSurfaceDark
import com.aigallery.app.core.theme.SpacingTokens
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.organization.model.ScreenshotCategory
import com.aigallery.app.presentation.home.MediaGridItem
import com.aigallery.app.presentation.selection.rememberSelectionState

@Composable
fun CategoryDetailScreen(
    category: ScreenshotCategory,
    viewModel: OrganizationDashboardViewModel,
    onBackClick: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubcategory by remember { mutableStateOf<String?>("All") }
    val mediaItems by viewModel.getCategoryMedia(category.id, selectedSubcategory)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val subcategoryCounts by viewModel.getSubcategoriesForCategory(category.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val selectionState = rememberSelectionState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Floating Top Header
            LiquidGlassToolbar(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = SpacingTokens.Section, vertical = SpacingTokens.Small)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBackClick
                            ),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .border(1.dp, AccentCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category.displayName,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${mediaItems.size} organized screenshots",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Subcategory Filter Chips Row
            if (subcategoryCounts.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "All" Chip
                    item {
                        SubcategoryFilterChip(
                            label = "All",
                            count = null,
                            isSelected = selectedSubcategory == null || selectedSubcategory == "All",
                            onClick = { selectedSubcategory = "All" }
                        )
                    }

                    items(
                        items = subcategoryCounts,
                        key = { it.subcategory }
                    ) { sub ->
                        SubcategoryFilterChip(
                            label = sub.subcategory,
                            count = sub.count,
                            isSelected = selectedSubcategory == sub.subcategory,
                            onClick = { selectedSubcategory = sub.subcategory }
                        )
                    }
                }
            }

            // Empty State if no media
            if (mediaItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOff,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No screenshots in ${category.displayName}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Screenshots detected with ${category.displayName.lowercase()} context will appear here automatically.",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                // 3-column Media Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 100.dp,
                        start = 4.dp,
                        end = 4.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = mediaItems,
                        key = { it.id }
                    ) { item ->
                        MediaGridItem(
                            item = item,
                            isSelected = selectionState.isSelected(item.id),
                            isInSelectionMode = selectionState.isInSelectionMode,
                            onClick = {
                                if (selectionState.isInSelectionMode) {
                                    selectionState.toggle(item.id)
                                } else {
                                    onMediaClick(item)
                                }
                            },
                            onLongClick = {
                                selectionState.toggle(item.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryFilterChip(
    label: String,
    count: Int?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (isSelected) {
                    Brush.horizontalGradient(
                        listOf(AccentCyan.copy(alpha = 0.35f), AccentPurple.copy(alpha = 0.35f))
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.04f))
                    )
                }
            )
            .border(
                1.dp,
                if (isSelected) AccentCyan.copy(alpha = 0.6f) else GlassBorderLight,
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (count != null && count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$count",
                    color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

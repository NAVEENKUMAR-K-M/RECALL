package com.aigallery.app.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigallery.app.core.theme.ColorTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.SpacingTokens
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.presentation.home.MediaGridItem
import com.aigallery.app.presentation.selection.rememberSelectionState

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val glassColors = LocalGlassColors.current
    val selectionState = rememberSelectionState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (glassColors.isDark) ColorTokens.DarkBackground else ColorTokens.LightBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = SpacingTokens.Section, vertical = SpacingTokens.Small)
            ) {
                Text(
                    text = "Favorites",
                    color = glassColors.textPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${favorites.size} saved memories",
                    color = glassColors.textTertiary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = glassColors.textTertiary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No favorites yet",
                            color = glassColors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the heart icon on any photo to save your favorite memories here.",
                            color = glassColors.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
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
                        items = favorites,
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

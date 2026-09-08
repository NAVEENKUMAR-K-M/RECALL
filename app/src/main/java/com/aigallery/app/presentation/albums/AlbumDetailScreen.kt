package com.aigallery.app.presentation.albums

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigallery.app.core.design.LiquidGlassToolbar
import com.aigallery.app.core.theme.ColorTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.SpacingTokens
import com.aigallery.app.domain.model.Album
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.presentation.home.MediaGridItem
import com.aigallery.app.presentation.selection.rememberSelectionState

@Composable
fun AlbumDetailScreen(
    album: Album,
    viewModel: AlbumsViewModel,
    onBackClick: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaItems by viewModel.getAlbumMedia(album.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val glassColors = LocalGlassColors.current
    val selectionState = rememberSelectionState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (glassColors.isDark) ColorTokens.DarkBackground else ColorTokens.LightBackground)
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
                        tint = glassColors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = album.name,
                            color = glassColors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${mediaItems.size} items",
                            color = glassColors.textTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // 3-column Grid
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

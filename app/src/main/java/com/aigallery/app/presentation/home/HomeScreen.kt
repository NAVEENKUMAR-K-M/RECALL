package com.aigallery.app.presentation.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigallery.app.core.design.LiquidGlassButton
import com.aigallery.app.core.design.LiquidGlassContainer
import com.aigallery.app.core.design.LiquidGlassSearchBar
import com.aigallery.app.core.theme.ColorTokens
import com.aigallery.app.core.theme.GlassTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.ShapeTokens
import com.aigallery.app.core.theme.SpacingTokens
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.presentation.selection.SelectionBottomBar
import com.aigallery.app.presentation.selection.SelectionState
import com.aigallery.app.presentation.selection.SelectionTopBar
import com.aigallery.app.presentation.selection.rememberSelectionState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMediaClick: (MediaItem) -> Unit,
    onSearchClick: () -> Unit,
    stats: com.aigallery.app.domain.repository.ScreenshotAIStats? = null,
    onIndexingStatusClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectionState = rememberSelectionState()
    val glassColors = LocalGlassColors.current
    val context = LocalContext.current

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (glassColors.isDark) ColorTokens.DarkBackground else ColorTokens.LightBackground)
    ) {
        // Chronological Photo Grid
        PhotoGrid(
            groups = uiState.groups,
            selectionState = selectionState,
            onItemClick = onMediaClick,
            onItemLongClick = { item ->
                selectionState.toggle(item.id)
            },
            contentPadding = PaddingValues(
                top = 165.dp,
                bottom = 100.dp, // Clearance for floating bottom nav
                start = 6.dp,
                end = 6.dp
            )
        )

        // Floating Top Header & Search Bar (hidden in selection mode)
        AnimatedVisibility(
            visible = !selectionState.isInSelectionMode,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                HomeHeader(totalItems = uiState.totalCount)

                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.padding(horizontal = SpacingTokens.Section)) {
                    LiquidGlassSearchBar(
                        readOnly = true,
                        onClick = onSearchClick,
                        placeholder = "Search photos, albums, text..."
                    )
                }
            }
        }

        // Selection Mode Top Bar
        AnimatedVisibility(
            visible = selectionState.isInSelectionMode,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            SelectionTopBar(
                count = selectionState.count,
                onCloseClick = { selectionState.clear() },
                onSelectAllClick = {
                    selectionState.selectAll(uiState.allMedia.map { it.id })
                }
            )
        }

        // Selection Mode Bottom Bar
        AnimatedVisibility(
            visible = selectionState.isInSelectionMode,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            SelectionBottomBar(
                onShareClick = {
                    val selectedItems = uiState.allMedia.filter { selectionState.isSelected(it.id) }
                    if (selectedItems.isNotEmpty()) {
                        val shareIntent = Intent().apply {
                            if (selectedItems.size == 1) {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_STREAM, selectedItems.first().uri)
                                type = selectedItems.first().mimeType
                            } else {
                                action = Intent.ACTION_SEND_MULTIPLE
                                putParcelableArrayListExtra(
                                    Intent.EXTRA_STREAM,
                                    ArrayList(selectedItems.map { it.uri })
                                )
                                type = "*/*"
                            }
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                    }
                },
                onDeleteClick = {
                    showDeleteConfirmDialog = true
                }
            )
        }

        // Delete Confirmation Dialog with Liquid Glass Styling
        if (showDeleteConfirmDialog) {
            val selectedItems = uiState.allMedia.filter { selectionState.isSelected(it.id) }
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = {
                    Text(
                        text = "Delete ${selectedItems.size} items?",
                        color = glassColors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to permanently remove these items from your device? This action cannot be undone.",
                        color = glassColors.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmDialog = false
                            viewModel.deleteItems(selectedItems) {
                                selectionState.clear()
                            }
                        }
                    ) {
                        Text(text = "Delete", color = ColorTokens.AccentHeart, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(text = "Cancel", color = glassColors.textSecondary)
                    }
                },
                containerColor = glassColors.surfaceHeavy,
                shape = ShapeTokens.Card
            )
        }
    }
}

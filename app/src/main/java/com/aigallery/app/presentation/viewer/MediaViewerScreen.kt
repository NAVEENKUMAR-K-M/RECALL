package com.aigallery.app.presentation.viewer

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.aigallery.app.ai.ScreenshotDetector
import com.aigallery.app.core.theme.AccentCyan
import com.aigallery.app.presentation.screenshot.ScreenshotInsightsSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigallery.app.core.design.LiquidGlassButton
import com.aigallery.app.core.design.LiquidGlassToolbar
import com.aigallery.app.core.theme.ColorTokens
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.ShapeTokens
import com.aigallery.app.core.theme.SpacingTokens
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.presentation.viewer.components.VideoPlayerView
import com.aigallery.app.presentation.viewer.components.ZoomableImageView

@Composable
fun MediaViewerScreen(
    items: List<MediaItem>,
    initialIndex: Int,
    viewModel: MediaViewerViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    // Intercept system back gesture to exit viewer cleanly
    BackHandler {
        onBackClick()
    }

    val context = LocalContext.current
    val glassColors = LocalGlassColors.current

    val safeInitialIndex = initialIndex.coerceIn(0, items.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitialIndex) { items.size }

    var showControls by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showInsightsSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val currentItem = items.getOrNull(pagerState.currentPage) ?: items.first()
    val metadata by viewModel.metadata.collectAsStateWithLifecycle()
    val screenshotAI by viewModel.currentScreenshotAI.collectAsStateWithLifecycle()
    val relatedScreenshots by viewModel.relatedScreenshots.collectAsStateWithLifecycle()
    val isScreenshot = ScreenshotDetector.isScreenshot(currentItem)
    val coroutineScope = rememberCoroutineScope()

    // Request metadata & screenshot intelligence on page change
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            items.getOrNull(page)?.let { viewModel.loadItemData(it) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)

    ) {
        // Horizontal Pager for swiping between media
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { index -> items.getOrNull(index)?.id ?: index }
        ) { page ->
            val mediaItem = items[page]
            if (mediaItem.isVideo) {
                VideoPlayerView(
                    item = mediaItem,
                    showControls = showControls,
                    onTap = { showControls = !showControls }
                )
            } else {
                ZoomableImageView(
                    item = mediaItem,
                    onTap = { showControls = !showControls },
                    onDismiss = onBackClick
                )
            }
        }

        // Top Floating Glass Toolbar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LiquidGlassToolbar(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = SpacingTokens.Section, vertical = SpacingTokens.Small),
                backgroundColor = Color(0x750D0D14),
                height = 56.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0x28FFFFFF))
                            .border(1.dp, Color(0x30FFFFFF), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBackClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} of ${items.size}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = currentItem.filename,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    // Heart Favorite Toggle with spring animation
                    val isFav = currentItem.isFavorite
                    val favScale by animateFloatAsState(
                        targetValue = if (isFav) 1.25f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
                        label = "favScale"
                    )

                    // Right: Favorite
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isFav) Color(0x33FF4081) else Color(0x28FFFFFF))
                            .border(1.dp, if (isFav) Color(0x55FF4081) else Color(0x30FFFFFF), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { viewModel.toggleFavorite(currentItem) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            modifier = Modifier
                                .size(20.dp)
                                .scale(favScale),
                            tint = if (isFav) ColorTokens.AccentHeart else Color.White
                        )
                    }
                }
            }
        }

        // Bottom Floating Glass Toolbar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            LiquidGlassToolbar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = SpacingTokens.Section, vertical = SpacingTokens.Small),
                backgroundColor = Color(0x750D0D14),
                height = 74.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewerActionButton(
                        icon = Icons.Rounded.Share,
                        label = "Share",
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                type = currentItem.mimeType
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                        }
                    )

                    ViewerActionButton(
                        icon = Icons.Rounded.Info,
                        label = "Details",
                        onClick = { showInfoSheet = true }
                    )

                    if (isScreenshot) {
                        ViewerActionButton(
                            icon = Icons.Rounded.DocumentScanner,
                            label = "Insights",
                            onClick = { showInsightsSheet = true }
                        )
                    }

                    ViewerActionButton(
                        icon = Icons.Rounded.Delete,
                        label = "Delete",
                        onClick = { showDeleteConfirm = true },
                        tint = ColorTokens.AccentHeart
                    )
                }
            }
        }

        // Info Bottom Sheet
        if (showInfoSheet) {
            InfoBottomSheet(
                item = currentItem,
                metadata = metadata,
                onDismissRequest = { showInfoSheet = false }
            )
        }

        // AI Insights Bottom Sheet
        if (showInsightsSheet) {
            ScreenshotInsightsSheet(
                screenshotWithAI = screenshotAI,
                relatedScreenshots = relatedScreenshots,
                onRelatedScreenshotClick = { targetMediaId ->
                    val targetIndex = items.indexOfFirst { it.id == targetMediaId }
                    if (targetIndex >= 0) {
                        showInsightsSheet = false
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetIndex)
                        }
                    }
                },
                onDismiss = { showInsightsSheet = false }
            )
        }


        // Delete Confirmation Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = {
                    Text(
                        text = "Delete item?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to permanently delete \"${currentItem.filename}\" from your device?",
                        color = Color(0xFFB0B0B8)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirm = false
                            viewModel.deleteItem(currentItem) {
                                if (items.size <= 1) {
                                    onBackClick()
                                }
                            }
                        }
                    ) {
                        Text(text = "Delete", color = ColorTokens.AccentHeart, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(text = "Cancel", color = Color.White)
                    }
                },
                containerColor = Color(0xFF16161E),
                shape = ShapeTokens.Card
            )
        }
    }
}

@Composable
private fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "actionBtnScale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF))
                .border(
                    width = 1.dp,
                    color = Color(0x2EFFFFFF),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = tint
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = if (tint == ColorTokens.AccentHeart) tint else Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

package com.aigallery.app.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aigallery.app.core.design.LiquidGlassChip
import com.aigallery.app.core.design.LiquidGlassSearchBar
import com.aigallery.app.core.theme.AccentCyan
import com.aigallery.app.core.theme.ColorTokens
import com.aigallery.app.core.theme.GlassBorderLight
import com.aigallery.app.core.theme.GlassSurfaceDark
import com.aigallery.app.core.theme.LocalGlassColors
import com.aigallery.app.core.theme.SpacingTokens
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.presentation.home.MediaGridItem
import com.aigallery.app.presentation.selection.rememberSelectionState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onMediaClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val queryText by viewModel.query.collectAsStateWithLifecycle()
    val glassColors = LocalGlassColors.current
    val selectionState = rememberSelectionState()
    val focusRequester = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
    var localQuery by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(queryText) }

    androidx.compose.runtime.LaunchedEffect(queryText) {
        if (localQuery != queryText) {
            localQuery = queryText
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (glassColors.isDark) ColorTokens.DarkBackground else ColorTokens.LightBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Area: Expanded Glass Search Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = SpacingTokens.Section, vertical = SpacingTokens.Small)
            ) {
                LiquidGlassSearchBar(
                    query = localQuery,
                    onQueryChange = {
                        localQuery = it
                        viewModel.onQueryChange(it)
                    },
                    onClearClick = {
                        localQuery = ""
                        viewModel.clearQuery()
                    },
                    placeholder = "Search screenshots, OCR text, entities...",
                    focusRequester = focusRequester
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchFilter.entries.forEach { filter ->
                        LiquidGlassChip(
                            label = filter.displayName,
                            selected = filter == uiState.activeFilter,
                            onClick = { viewModel.onFilterSelect(filter) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Recent Searches when empty query
            if (uiState.query.isEmpty() && uiState.activeFilter == SearchFilter.ALL) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Recent Searches",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.recentSearches.forEach { term ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GlassSurfaceDark)
                                    .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                                    .clickable { viewModel.onQueryChange(term) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = term,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Results count or empty state
            if (uiState.results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (uiState.isSearching) Icons.Rounded.SearchOff else Icons.Rounded.Search,
                            contentDescription = null,
                            tint = glassColors.textTertiary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (uiState.isSearching) "No matching photos found" else "Search photos, albums & screenshots",
                            color = if (uiState.isSearching) Color.White else glassColors.textSecondary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (uiState.isSearching) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Try searching by keyword, topic, date or text",
                                color = glassColors.textTertiary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 4.dp,
                        bottom = 100.dp,
                        start = 4.dp,
                        end = 4.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiState.results,
                        key = { it.mediaItem.id }
                    ) { resultItem ->
                        MediaGridItem(
                            item = resultItem.mediaItem,
                            isSelected = selectionState.isSelected(resultItem.mediaItem.id),
                            isInSelectionMode = selectionState.isInSelectionMode,
                            onClick = {
                                if (selectionState.isInSelectionMode) {
                                    selectionState.toggle(resultItem.mediaItem.id)
                                } else {
                                    onMediaClick(resultItem.mediaItem)
                                }
                            },
                            onLongClick = {
                                selectionState.toggle(resultItem.mediaItem.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

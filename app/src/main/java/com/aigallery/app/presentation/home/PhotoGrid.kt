package com.aigallery.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aigallery.app.domain.model.MediaGroup
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.presentation.selection.SelectionState

@Composable
fun PhotoGrid(
    groups: List<MediaGroup>,
    selectionState: SelectionState,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        groups.forEach { group ->
            item(
                key = "header_${group.title}",
                span = { GridItemSpan(3) }
            ) {
                ChronologicalHeader(
                    title = group.title,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            items(
                items = group.items,
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
                            onItemClick(item)
                        }
                    },
                    onLongClick = {
                        onItemLongClick(item)
                    }
                )
            }
        }
    }
}

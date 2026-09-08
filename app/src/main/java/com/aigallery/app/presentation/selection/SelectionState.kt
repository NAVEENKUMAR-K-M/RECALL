package com.aigallery.app.presentation.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class SelectionState {
    var selectedIds by mutableStateOf<Set<Long>>(emptySet())
        private set

    val isInSelectionMode: Boolean get() = selectedIds.isNotEmpty()
    val count: Int get() = selectedIds.size

    fun toggle(id: Long) {
        selectedIds = if (selectedIds.contains(id)) {
            selectedIds - id
        } else {
            selectedIds + id
        }
    }

    fun selectAll(ids: Collection<Long>) {
        selectedIds = ids.toSet()
    }

    fun clear() {
        selectedIds = emptySet()
    }

    fun isSelected(id: Long): Boolean = selectedIds.contains(id)
}

@Composable
fun rememberSelectionState(): SelectionState {
    return remember { SelectionState() }
}

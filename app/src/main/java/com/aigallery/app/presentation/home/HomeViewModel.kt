package com.aigallery.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aigallery.app.domain.model.MediaGroup
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.repository.FavoritesRepository
import com.aigallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val groups: List<MediaGroup> = emptyList(),
    val allMedia: List<MediaItem> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

class HomeViewModel(
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        mediaRepository.getGroupedMedia(),
        mediaRepository.getAllMedia()
    ) { groups, allMedia ->
        HomeUiState(
            groups = groups,
            allMedia = allMedia,
            totalCount = allMedia.size,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun toggleFavorite(mediaId: Long) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(mediaId)
        }
    }

    fun deleteItems(items: List<MediaItem>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            mediaRepository.deleteMedia(items)
            onComplete()
        }
    }

    class Factory(
        private val mediaRepository: MediaRepository,
        private val favoritesRepository: FavoritesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(mediaRepository, favoritesRepository) as T
        }
    }
}

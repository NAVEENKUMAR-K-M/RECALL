package com.aigallery.app.presentation.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aigallery.app.domain.model.Album
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AlbumsViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    val albums: StateFlow<List<Album>> = mediaRepository.getAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getAlbumMedia(albumId: Long): Flow<List<MediaItem>> {
        return mediaRepository.getMediaByAlbum(albumId)
    }

    class Factory(
        private val mediaRepository: MediaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AlbumsViewModel(mediaRepository) as T
        }
    }
}

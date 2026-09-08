package com.aigallery.app.presentation.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aigallery.app.ai.ScreenshotDetector
import com.aigallery.app.data.database.ScreenshotWithAI
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.model.MediaMetadata
import com.aigallery.app.domain.repository.FavoritesRepository
import com.aigallery.app.domain.repository.MediaRepository
import com.aigallery.app.domain.repository.ScreenshotRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MediaViewerViewModel(
    private val mediaRepository: MediaRepository,
    private val favoritesRepository: FavoritesRepository,
    private val screenshotRepository: ScreenshotRepository
) : ViewModel() {

    private val _metadata = MutableStateFlow<MediaMetadata?>(null)
    val metadata: StateFlow<MediaMetadata?> = _metadata.asStateFlow()

    private val _currentScreenshotAI = MutableStateFlow<ScreenshotWithAI?>(null)
    val currentScreenshotAI: StateFlow<ScreenshotWithAI?> = _currentScreenshotAI.asStateFlow()

    private val _relatedScreenshots = MutableStateFlow<List<com.aigallery.app.data.database.RelatedScreenshotItem>>(emptyList())
    val relatedScreenshots: StateFlow<List<com.aigallery.app.data.database.RelatedScreenshotItem>> = _relatedScreenshots.asStateFlow()

    private var aiJob: Job? = null

    fun loadItemData(item: MediaItem) {
        viewModelScope.launch {
            _metadata.value = mediaRepository.getMetadata(item)
        }

        aiJob?.cancel()
        if (ScreenshotDetector.isScreenshot(item)) {
            aiJob = viewModelScope.launch {
                screenshotRepository.getScreenshotWithAI(item.id).collect { ai ->
                    _currentScreenshotAI.value = ai
                    if (ai != null) {
                        launch {
                            screenshotRepository.getRelatedScreenshots(ai.screenshot.screenshotId).collect { related ->
                                _relatedScreenshots.value = related
                            }
                        }
                    } else {
                        _relatedScreenshots.value = emptyList()
                    }
                }
            }
        } else {
            _currentScreenshotAI.value = null
            _relatedScreenshots.value = emptyList()
        }
    }


    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            favoritesRepository.toggleFavorite(item.id)
        }
    }

    fun deleteItem(item: MediaItem, onComplete: () -> Unit) {
        viewModelScope.launch {
            mediaRepository.deleteMedia(listOf(item))
            onComplete()
        }
    }

    class Factory(
        private val mediaRepository: MediaRepository,
        private val favoritesRepository: FavoritesRepository,
        private val screenshotRepository: ScreenshotRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MediaViewerViewModel(mediaRepository, favoritesRepository, screenshotRepository) as T
        }
    }
}

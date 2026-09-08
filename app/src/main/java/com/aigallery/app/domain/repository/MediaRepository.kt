package com.aigallery.app.domain.repository

import com.aigallery.app.domain.model.Album
import com.aigallery.app.domain.model.MediaGroup
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.model.MediaMetadata
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllMedia(): Flow<List<MediaItem>>
    fun getGroupedMedia(): Flow<List<MediaGroup>>
    fun getAlbums(): Flow<List<Album>>
    fun getMediaByAlbum(albumId: Long): Flow<List<MediaItem>>
    suspend fun getMetadata(item: MediaItem): MediaMetadata
    suspend fun deleteMedia(items: List<MediaItem>): Result<Unit>
}

interface FavoritesRepository {
    fun getFavorites(): Flow<List<MediaItem>>
    fun isFavorite(mediaId: Long): Flow<Boolean>
    suspend fun toggleFavorite(mediaId: Long)
}

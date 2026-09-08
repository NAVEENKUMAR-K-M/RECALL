package com.aigallery.app.data.repository

import android.content.Context
import com.aigallery.app.core.utils.DateTimeUtils
import com.aigallery.app.data.database.FavoriteDao
import com.aigallery.app.data.media.ExifDataSource
import com.aigallery.app.data.media.MediaContentObserver
import com.aigallery.app.data.media.MediaStoreDataSource
import com.aigallery.app.domain.model.Album
import com.aigallery.app.domain.model.MediaGroup
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.model.MediaMetadata
import com.aigallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MediaRepositoryImpl(
    private val context: Context,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val mediaContentObserver: MediaContentObserver,
    private val favoriteDao: FavoriteDao,
    private val exifDataSource: ExifDataSource
) : MediaRepository {

    override fun getAllMedia(): Flow<List<MediaItem>> {
        val rawMediaFlow = mediaContentObserver.observeMediaChanges().map {
            mediaStoreDataSource.queryAllMedia()
        }

        return combine(rawMediaFlow, favoriteDao.getAllFavoriteIds()) { mediaList, favIds ->
            val favSet = favIds.toSet()
            mediaList.map { item ->
                if (item.id in favSet) item.copy(isFavorite = true) else item
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getGroupedMedia(): Flow<List<MediaGroup>> {
        return getAllMedia().map { allMedia ->
            val groups = mutableListOf<MediaGroup>()
            var currentTitle = ""
            var currentList = mutableListOf<MediaItem>()

            for (item in allMedia) {
                val groupTitle = DateTimeUtils.formatGroupTitle(item.dateTaken)
                if (groupTitle != currentTitle) {
                    if (currentList.isNotEmpty()) {
                        groups.add(MediaGroup(title = currentTitle, items = currentList.toList()))
                        currentList = mutableListOf()
                    }
                    currentTitle = groupTitle
                }
                currentList.add(item)
            }

            if (currentList.isNotEmpty()) {
                groups.add(MediaGroup(title = currentTitle, items = currentList.toList()))
            }

            groups
        }.flowOn(Dispatchers.Default)
    }

    override fun getAlbums(): Flow<List<Album>> {
        return getAllMedia().map { allMedia ->
            mediaStoreDataSource.queryAlbums(allMedia)
        }.flowOn(Dispatchers.Default)
    }

    override fun getMediaByAlbum(albumId: Long): Flow<List<MediaItem>> {
        return getAllMedia().map { allMedia ->
            allMedia.filter { it.bucketId == albumId }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun getMetadata(item: MediaItem): MediaMetadata {
        return exifDataSource.getMetadata(item)
    }

    override suspend fun deleteMedia(items: List<MediaItem>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            for (item in items) {
                contentResolver.delete(item.uri, null, null)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

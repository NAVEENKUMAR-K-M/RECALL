package com.aigallery.app.data.repository

import com.aigallery.app.data.database.FavoriteDao
import com.aigallery.app.data.database.FavoriteEntity
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.repository.FavoritesRepository
import com.aigallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class FavoritesRepositoryImpl(
    private val favoriteDao: FavoriteDao,
    private val mediaRepository: () -> MediaRepository
) : FavoritesRepository {

    override fun getFavorites(): Flow<List<MediaItem>> {
        return combine(
            mediaRepository().getAllMedia(),
            favoriteDao.getAllFavoriteIds()
        ) { allMedia, favIds ->
            val favSet = favIds.toSet()
            allMedia.filter { it.id in favSet }.map { it.copy(isFavorite = true) }
        }
    }

    override fun isFavorite(mediaId: Long): Flow<Boolean> {
        return favoriteDao.isFavorite(mediaId)
    }

    override suspend fun toggleFavorite(mediaId: Long) {
        val isFav = favoriteDao.isFavorite(mediaId).first()
        if (isFav) {
            favoriteDao.deleteFavorite(mediaId)
        } else {
            favoriteDao.insertFavorite(FavoriteEntity(mediaId = mediaId))
        }
    }
}

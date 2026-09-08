package com.aigallery.app.domain.repository

import com.aigallery.app.data.database.RelatedScreenshotItem
import com.aigallery.app.data.database.ScreenshotCollectionEntity
import com.aigallery.app.data.database.ScreenshotEmbeddingEntity
import com.aigallery.app.data.database.ScreenshotWithAI
import com.aigallery.app.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

data class ScreenshotAIStats(
    val totalCount: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val failedCount: Int = 0,
    val textCount: Int = 0,
    val entityCount: Int = 0,
    val tagCount: Int = 0,
    val llmCount: Int = 0,
    val embeddingCount: Int = 0,
    val relationCount: Int = 0
)

interface ScreenshotRepository {
    suspend fun syncScreenshots(mediaItems: List<MediaItem>)
    fun getScreenshotWithAI(mediaId: Long): Flow<ScreenshotWithAI?>
    suspend fun getScreenshotWithAISync(mediaId: Long): ScreenshotWithAI?
    fun searchScreenshots(query: String): Flow<List<ScreenshotWithAI>>
    fun getScreenshotsByCategory(category: String): Flow<List<ScreenshotWithAI>>
    fun getScreenshotsByPlatform(platform: String): Flow<List<ScreenshotWithAI>>
    fun getAllCollections(): Flow<List<ScreenshotCollectionEntity>>
    fun getScreenshotsForCollection(collectionId: String): Flow<List<ScreenshotWithAI>>
    fun getRelatedScreenshots(screenshotId: Long): Flow<List<RelatedScreenshotItem>>
    suspend fun getAllEmbeddings(): List<ScreenshotEmbeddingEntity>
    fun getAIStats(): Flow<ScreenshotAIStats>
    suspend fun reprocessAll()
}


package com.aigallery.app.data.repository

import android.content.Context
import com.aigallery.app.ai.ScreenshotDetector
import com.aigallery.app.data.database.AppDatabase
import com.aigallery.app.data.database.ProcessingStatus
import com.aigallery.app.data.database.ScreenshotCollectionEntity
import com.aigallery.app.data.database.ScreenshotEntity
import com.aigallery.app.data.database.ScreenshotWithAI
import com.aigallery.app.data.worker.ScreenshotProcessingWorker
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.repository.ScreenshotAIStats
import com.aigallery.app.domain.repository.ScreenshotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class ScreenshotRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getInstance(context)
) : ScreenshotRepository {

    private val screenshotDao = database.screenshotDao()
    private val currentModelVersion = 1

    override suspend fun syncScreenshots(mediaItems: List<MediaItem>) = withContext(Dispatchers.IO) {
        var hasNewPending = false

        // STRICT FILTER: Only screenshots are allowed into this pipeline.
        // Camera photos, WhatsApp media, and downloads are completely filtered out.
        val detectedScreenshots = mediaItems.filter { ScreenshotDetector.isScreenshot(it) }

        for (item in detectedScreenshots) {
            val existing = screenshotDao.getScreenshotByMediaId(item.id)

            if (existing == null) {
                // Brand new screenshot
                screenshotDao.insertScreenshot(
                    ScreenshotEntity(
                        mediaId = item.id,
                        uri = item.uri.toString(),
                        filename = item.filename,
                        createdAt = item.dateTaken,
                        width = item.width,
                        height = item.height,
                        fileSize = item.size,
                        relativePath = item.relativePath,
                        processingStatus = ProcessingStatus.PENDING,
                        modelVersion = currentModelVersion
                    )
                )
                hasNewPending = true
            } else if (existing.modelVersion < currentModelVersion) {
                // Incremental processing: re-queue if pipeline/model version upgraded
                screenshotDao.updateProcessingStatus(
                    screenshotId = existing.screenshotId,
                    status = ProcessingStatus.PENDING
                )
                hasNewPending = true
            }
        }

        // Check if unfinished work remains from prior app sessions
        val unfinished = screenshotDao.getUnfinishedScreenshots()
        if (hasNewPending || unfinished.isNotEmpty()) {
            ScreenshotProcessingWorker.enqueue(context)
        }
    }

    override fun getScreenshotWithAI(mediaId: Long): Flow<ScreenshotWithAI?> {
        return screenshotDao.getScreenshotWithAIByMediaIdFlow(mediaId)
    }

    override suspend fun getScreenshotWithAISync(mediaId: Long): ScreenshotWithAI? {
        return screenshotDao.getScreenshotWithAIByMediaIdSync(mediaId)
    }

    override fun searchScreenshots(query: String): Flow<List<ScreenshotWithAI>> {
        return screenshotDao.searchScreenshots(query.trim())
    }

    override fun getScreenshotsByCategory(category: String): Flow<List<ScreenshotWithAI>> {
        return screenshotDao.getScreenshotsByCategory(category)
    }

    override fun getScreenshotsByPlatform(platform: String): Flow<List<ScreenshotWithAI>> {
        return screenshotDao.getScreenshotsByPlatform(platform)
    }

    override fun getAllCollections(): Flow<List<ScreenshotCollectionEntity>> {
        return screenshotDao.getAllCollectionsFlow()
    }

    override fun getScreenshotsForCollection(collectionId: String): Flow<List<ScreenshotWithAI>> {
        return screenshotDao.getScreenshotsForCollectionFlow(collectionId)
    }

    override fun getRelatedScreenshots(screenshotId: Long): Flow<List<com.aigallery.app.data.database.RelatedScreenshotItem>> {
        return screenshotDao.getRelatedScreenshotsFlow(screenshotId)
    }

    override suspend fun getAllEmbeddings(): List<com.aigallery.app.data.database.ScreenshotEmbeddingEntity> {
        return screenshotDao.getAllEmbeddingsSync()
    }

    override fun getAIStats(): Flow<ScreenshotAIStats> {
        val flows: List<Flow<Int>> = listOf(
            screenshotDao.getTotalCountFlow(),
            screenshotDao.getCompletedCountFlow(),
            screenshotDao.getPendingCountFlow(),
            screenshotDao.getFailedCountFlow(),
            screenshotDao.getTextCountFlow(),
            screenshotDao.getEntityCountFlow(),
            screenshotDao.getTagCountFlow(),
            screenshotDao.getLLMCountFlow(),
            screenshotDao.getEmbeddingCountFlow(),
            screenshotDao.getRelationsCountFlow()
        )
        return combine(flows) { counts ->
            ScreenshotAIStats(
                totalCount = counts[0],
                completedCount = counts[1],
                pendingCount = counts[2],
                failedCount = counts[3],
                textCount = counts[4],
                entityCount = counts[5],
                tagCount = counts[6],
                llmCount = counts[7],
                embeddingCount = counts[8],
                relationCount = counts[9]
            )
        }
    }

    override suspend fun reprocessAll() = withContext(Dispatchers.IO) {
        screenshotDao.resetAllForReprocessing()
        ScreenshotProcessingWorker.enqueue(context)
    }
}


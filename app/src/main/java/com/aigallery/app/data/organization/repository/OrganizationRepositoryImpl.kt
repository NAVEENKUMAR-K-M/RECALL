package com.aigallery.app.data.organization.repository

import android.content.Context
import android.net.Uri
import com.aigallery.app.ai.organization.ScreenshotOrganizationEngine
import com.aigallery.app.ai.organization.ScreenshotOrganizationEngineImpl
import com.aigallery.app.data.database.AppDatabase
import com.aigallery.app.data.database.CategoryCountResult
import com.aigallery.app.data.database.OrganizedMediaEntity
import com.aigallery.app.data.database.OrganizationStatus
import com.aigallery.app.data.database.OrganizationStatsResult
import com.aigallery.app.data.database.ScreenshotCollectionEntity
import com.aigallery.app.data.database.SubcategoryCountResult
import com.aigallery.app.data.storage.MediaStoreOrganizationManager
import com.aigallery.app.data.worker.ScreenshotOrganizationWorker
import com.aigallery.app.domain.organization.model.ScreenshotCategory
import com.aigallery.app.domain.organization.repository.OrganizationRepository
import com.aigallery.app.domain.organization.repository.OrganizationSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class OrganizationRepositoryImpl(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: OrganizationSettingsRepository,
    private val mediaStoreManager: MediaStoreOrganizationManager,
    private val organizationEngine: ScreenshotOrganizationEngine = ScreenshotOrganizationEngineImpl()
) : OrganizationRepository {

    private val organizationDao = database.organizationDao()
    private val screenshotDao = database.screenshotDao()

    override fun getOrganizationStats(): Flow<OrganizationStatsResult> {
        return combine(
            screenshotDao.getCompletedCountFlow(),
            organizationDao.getOrganizedCountFlow(),
            organizationDao.getPendingCountFlow(),
            organizationDao.getUnsortedCountFlow()
        ) { understood, organized, pending, unsorted ->
            OrganizationStatsResult(
                totalUnderstood = understood,
                totalOrganized = organized,
                totalPending = pending,
                totalUnsorted = unsorted
            )
        }
    }

    override fun getCategoryCounts(): Flow<List<CategoryCountResult>> {
        return organizationDao.getOrganizedCategoryCountsFlow()
    }

    override fun getSubcategoryCounts(): Flow<List<SubcategoryCountResult>> {
        return organizationDao.getSubcategoryCountsFlow()
    }

    override fun getOrganizedMediaByCategory(category: String): Flow<List<OrganizedMediaEntity>> {
        return organizationDao.getOrganizedMediaByCategoryFlow(category)
    }

    override fun getOrganizedMediaBySubcategory(category: String, subcategory: String): Flow<List<OrganizedMediaEntity>> {
        return organizationDao.getOrganizedMediaBySubcategoryFlow(category, subcategory)
    }

    override fun getOrganizedMedia(sourceMediaId: Long): Flow<OrganizedMediaEntity?> {
        return organizationDao.getOrganizedMediaFlow(sourceMediaId)
    }

    override suspend fun syncPendingOrganizeFromIntelligence() = withContext(Dispatchers.IO) {
        val completedScreenshots = screenshotDao.getCompletedScreenshots()
        val newPendingEntities = mutableListOf<OrganizedMediaEntity>()

        for (screenshot in completedScreenshots) {
            val existing = organizationDao.getOrganizedMedia(screenshot.mediaId)
            if (existing == null) {
                newPendingEntities.add(
                    OrganizedMediaEntity(
                        sourceMediaId = screenshot.mediaId,
                        sourceUri = screenshot.uri,
                        destinationUri = null,
                        category = ScreenshotCategory.OTHER.id,
                        subcategory = null,
                        status = OrganizationStatus.PENDING,
                        organizationVersion = 1,
                        createdAt = screenshot.createdAt,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        if (newPendingEntities.isNotEmpty()) {
            organizationDao.insertOrUpdateAll(newPendingEntities)
        }
    }

    override suspend fun organizePendingBatch(limit: Int): Int = withContext(Dispatchers.IO) {
        val pending = organizationDao.getPendingOrganization(limit)
        if (pending.isEmpty()) return@withContext 0

        val settings = settingsRepository.getSettings()
        var processedCount = 0

        for (item in pending) {
            val analysis = screenshotDao.getScreenshotWithAIByMediaIdSync(item.sourceMediaId)
            if (analysis == null) {
                continue
            }

            // Run AI Organization Engine
            val orgResult = organizationEngine.organize(
                screenshot = analysis.screenshot,
                analysis = analysis,
                minScreenshotsForSubfolder = settings.minScreenshotsForSubfolder,
                organizeLowConfidence = settings.isOrganizeLowConfidenceEnabled
            )

            var destinationUri = item.destinationUri

            // Physical Organization: Create scoped representation if enabled
            if (settings.isAutoOrganizationEnabled && orgResult.primaryCategory != ScreenshotCategory.OTHER) {
                // Idempotency: verify if destination copy already exists
                val alreadyExists = destinationUri?.let { mediaStoreManager.checkUriExists(Uri.parse(it)) } ?: false

                if (!alreadyExists) {
                    val createResult = mediaStoreManager.createOrganizedCopy(
                        sourceUri = Uri.parse(item.sourceUri),
                        filename = analysis.screenshot.filename,
                        mimeType = "image/png",
                        category = orgResult.primaryCategory,
                        subcategory = orgResult.subcategory
                    )
                    destinationUri = createResult.getOrNull()?.toString()
                }
            } else if (!settings.isAutoOrganizationEnabled) {
                // Virtual-only mode: clean destinationUri reference
                destinationUri = null
            }

            // Save updated organization record
            organizationDao.insertOrUpdate(
                item.copy(
                    category = orgResult.primaryCategory.id,
                    subcategory = orgResult.subcategory,
                    destinationUri = destinationUri,
                    confidence = orgResult.confidence,
                    reason = orgResult.reason,
                    status = OrganizationStatus.ORGANIZED,
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Auto-link primary and secondary categories to Virtual Collections
            val primaryColId = "col_${orgResult.primaryCategory.id.lowercase()}"
            screenshotDao.insertCollection(
                ScreenshotCollectionEntity(
                    collectionId = primaryColId,
                    name = orgResult.primaryCategory.displayName,
                    type = "CATEGORY",
                    description = orgResult.primaryCategory.description,
                    confidence = orgResult.confidence
                )
            )
            screenshotDao.linkScreenshotToCollection(primaryColId, analysis.screenshot.screenshotId)

            for (sec in orgResult.secondaryCategories) {
                val secColId = "col_${sec.id.lowercase()}"
                screenshotDao.insertCollection(
                    ScreenshotCollectionEntity(
                        collectionId = secColId,
                        name = sec.displayName,
                        type = "CATEGORY",
                        description = sec.description,
                        confidence = orgResult.confidence
                    )
                )
                screenshotDao.linkScreenshotToCollection(secColId, analysis.screenshot.screenshotId)
            }

            processedCount++
        }

        processedCount
    }

    override suspend fun organizeNow() = withContext(Dispatchers.IO) {
        syncPendingOrganizeFromIntelligence()
        ScreenshotOrganizationWorker.enqueue(context)
    }

    override suspend fun reorganizeAll() = withContext(Dispatchers.IO) {
        syncPendingOrganizeFromIntelligence()
        organizationDao.resetAllForReorganization()
        ScreenshotOrganizationWorker.enqueue(context)
    }

    override suspend fun reconcileExternalChanges() = withContext(Dispatchers.IO) {
        val allWithDest = organizationDao.getAllWithDestinationUri()
        for (item in allWithDest) {
            val destUri = item.destinationUri ?: continue
            val exists = mediaStoreManager.checkUriExists(Uri.parse(destUri))
            if (!exists) {
                // User deleted organized copy externally: mark STALE
                organizationDao.markStale(item.sourceMediaId)
            }
        }
    }

    override suspend fun cleanAllOrganizedCopies(): Int = withContext(Dispatchers.IO) {
        val deletedCount = mediaStoreManager.cleanAllOrganizedCopies()
        organizationDao.resetAllForReorganization()
        deletedCount
    }
}

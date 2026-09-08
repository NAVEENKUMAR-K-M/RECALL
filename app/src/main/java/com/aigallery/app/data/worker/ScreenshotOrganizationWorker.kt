package com.aigallery.app.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aigallery.app.data.database.AppDatabase
import com.aigallery.app.data.organization.repository.OrganizationRepositoryImpl
import com.aigallery.app.data.organization.repository.OrganizationSettingsRepositoryImpl
import com.aigallery.app.data.storage.MediaStoreOrganizationManagerImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenshotOrganizationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = AppDatabase.getInstance(appContext)
    private val settingsRepository = OrganizationSettingsRepositoryImpl(appContext)
    private val mediaStoreManager = MediaStoreOrganizationManagerImpl(appContext)

    private val organizationRepository = OrganizationRepositoryImpl(
        context = appContext,
        database = database,
        settingsRepository = settingsRepository,
        mediaStoreManager = mediaStoreManager
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            android.util.Log.i("OrgWorker", "Starting ScreenshotOrganizationWorker...")

            // 1. Sync pending from Phase 2 intelligence
            organizationRepository.syncPendingOrganizeFromIntelligence()

            // 2. Process in batches
            var totalProcessed = 0
            while (!isStopped) {
                val processedThisBatch = organizationRepository.organizePendingBatch(limit = 15)
                if (processedThisBatch <= 0) break
                totalProcessed += processedThisBatch
                android.util.Log.i("OrgWorker", "Organized batch: $processedThisBatch (Total so far: $totalProcessed)")
            }

            android.util.Log.i("OrgWorker", "ScreenshotOrganizationWorker finished successfully. Total: $totalProcessed")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("OrgWorker", "Error during organization worker", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "ScreenshotOrganizationWork"

        fun enqueue(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<ScreenshotOrganizationWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}

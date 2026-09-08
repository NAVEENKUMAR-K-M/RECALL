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

class OrganizationReconciliationWorker(
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
            android.util.Log.i("OrgReconcileWorker", "Starting reconciliation...")
            organizationRepository.reconcileExternalChanges()
            android.util.Log.i("OrgReconcileWorker", "Reconciliation completed.")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("OrgReconcileWorker", "Error reconciling organized media", e)
            Result.failure()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "OrganizationReconciliationWork"

        fun enqueue(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<OrganizationReconciliationWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}

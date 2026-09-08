package com.aigallery.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.aigallery.app.data.database.AppDatabase
import com.aigallery.app.data.media.ExifDataSource
import com.aigallery.app.data.media.MediaContentObserver
import com.aigallery.app.data.media.MediaStoreDataSource
import com.aigallery.app.data.repository.FavoritesRepositoryImpl
import com.aigallery.app.data.repository.MediaRepositoryImpl
import com.aigallery.app.domain.repository.FavoritesRepository
import com.aigallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AIGalleryApp : Application(), ImageLoaderFactory {

    lateinit var mediaRepository: MediaRepository
        private set

    lateinit var favoritesRepository: FavoritesRepository
        private set

    lateinit var screenshotRepository: com.aigallery.app.domain.repository.ScreenshotRepository
        private set

    lateinit var organizationRepository: com.aigallery.app.domain.organization.repository.OrganizationRepository
        private set

    lateinit var organizationSettingsRepository: com.aigallery.app.domain.organization.repository.OrganizationSettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val database = AppDatabase.getInstance(this)
        val mediaStoreDataSource = MediaStoreDataSource(this)
        val mediaContentObserver = MediaContentObserver(this)
        val exifDataSource = ExifDataSource(this)

        mediaRepository = MediaRepositoryImpl(
            context = this,
            mediaStoreDataSource = mediaStoreDataSource,
            mediaContentObserver = mediaContentObserver,
            favoriteDao = database.favoriteDao(),
            exifDataSource = exifDataSource
        )

        favoritesRepository = FavoritesRepositoryImpl(
            favoriteDao = database.favoriteDao(),
            mediaRepository = { mediaRepository }
        )

        screenshotRepository = com.aigallery.app.data.repository.ScreenshotRepositoryImpl(
            context = this,
            database = database
        )

        val settingsRepo = com.aigallery.app.data.organization.repository.OrganizationSettingsRepositoryImpl(this)
        organizationSettingsRepository = settingsRepo

        val mediaStoreManager = com.aigallery.app.data.storage.MediaStoreOrganizationManagerImpl(this)
        organizationRepository = com.aigallery.app.data.organization.repository.OrganizationRepositoryImpl(
            context = this,
            database = database,
            settingsRepository = settingsRepo,
            mediaStoreManager = mediaStoreManager,
            organizationEngine = com.aigallery.app.ai.organization.ScreenshotOrganizationEngineImpl()
        )

        // Clean any duplicate copies created during previous loop and reset organization state
        CoroutineScope(Dispatchers.IO).launch {
            try {
                mediaStoreManager.cleanAllOrganizedCopies()
                database.organizationDao().resetAllForReorganization()
            } catch (e: Exception) {
                // Safe ignore
            }
        }

        // Incrementally synchronize detected screenshots with WorkManager processing queue
        CoroutineScope(Dispatchers.IO).launch {
            mediaRepository.getAllMedia().collect { items ->
                screenshotRepository.syncScreenshots(items)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}

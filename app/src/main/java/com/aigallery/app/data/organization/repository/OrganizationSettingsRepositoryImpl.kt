package com.aigallery.app.data.organization.repository

import android.content.Context
import android.content.SharedPreferences
import com.aigallery.app.domain.organization.repository.OrganizationSettings
import com.aigallery.app.domain.organization.repository.OrganizationSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class OrganizationSettingsRepositoryImpl(
    context: Context
) : OrganizationSettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "ai_organization_settings",
        Context.MODE_PRIVATE
    )

    private val _settingsFlow = MutableStateFlow(loadSettings())

    override val settingsFlow: Flow<OrganizationSettings> = _settingsFlow.asStateFlow()

    override suspend fun getSettings(): OrganizationSettings = _settingsFlow.value

    override suspend fun updateSettings(settings: OrganizationSettings) {
        prefs.edit()
            .putBoolean(KEY_AUTO_ORGANIZE, settings.isAutoOrganizationEnabled)
            .putBoolean(KEY_SMART_SUBFOLDERS, settings.isSmartSubfoldersEnabled)
            .putBoolean(KEY_PRESERVE_ORIGINALS, settings.isPreserveOriginalsEnabled)
            .putBoolean(KEY_LOW_CONFIDENCE, settings.isOrganizeLowConfidenceEnabled)
            .putInt(KEY_MIN_FOR_SUBFOLDER, settings.minScreenshotsForSubfolder)
            .apply()
        updateFlow()
    }

    override suspend fun setAutoOrganizationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ORGANIZE, enabled).apply()
        updateFlow()
    }

    override suspend fun setSmartSubfoldersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_SUBFOLDERS, enabled).apply()
        updateFlow()
    }

    override suspend fun setPreserveOriginalsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRESERVE_ORIGINALS, enabled).apply()
        updateFlow()
    }

    override suspend fun setOrganizeLowConfidenceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOW_CONFIDENCE, enabled).apply()
        updateFlow()
    }

    override suspend fun setMinScreenshotsForSubfolder(min: Int) {
        prefs.edit().putInt(KEY_MIN_FOR_SUBFOLDER, min).apply()
        updateFlow()
    }

    private fun loadSettings(): OrganizationSettings {
        return OrganizationSettings(
            isAutoOrganizationEnabled = prefs.getBoolean(KEY_AUTO_ORGANIZE, false),
            isSmartSubfoldersEnabled = prefs.getBoolean(KEY_SMART_SUBFOLDERS, true),
            isPreserveOriginalsEnabled = prefs.getBoolean(KEY_PRESERVE_ORIGINALS, true),
            isOrganizeLowConfidenceEnabled = prefs.getBoolean(KEY_LOW_CONFIDENCE, false),
            minScreenshotsForSubfolder = prefs.getInt(KEY_MIN_FOR_SUBFOLDER, 3)
        )
    }

    private fun updateFlow() {
        _settingsFlow.value = loadSettings()
    }

    companion object {
        private const val KEY_AUTO_ORGANIZE = "key_auto_organize"
        private const val KEY_SMART_SUBFOLDERS = "key_smart_subfolders"
        private const val KEY_PRESERVE_ORIGINALS = "key_preserve_originals"
        private const val KEY_LOW_CONFIDENCE = "key_low_confidence"
        private const val KEY_MIN_FOR_SUBFOLDER = "key_min_for_subfolder"
    }
}

package com.aigallery.app.domain.organization.repository

import kotlinx.coroutines.flow.Flow

data class OrganizationSettings(
    val isAutoOrganizationEnabled: Boolean = false,
    val isSmartSubfoldersEnabled: Boolean = true,
    val isPreserveOriginalsEnabled: Boolean = true,
    val isOrganizeLowConfidenceEnabled: Boolean = false,
    val minScreenshotsForSubfolder: Int = 3
) {
    val isCreateSubfoldersEnabled: Boolean get() = isSmartSubfoldersEnabled
}

interface OrganizationSettingsRepository {
    val settingsFlow: Flow<OrganizationSettings>
    suspend fun getSettings(): OrganizationSettings
    suspend fun updateSettings(settings: OrganizationSettings)
    suspend fun setAutoOrganizationEnabled(enabled: Boolean)
    suspend fun setSmartSubfoldersEnabled(enabled: Boolean)
    suspend fun setPreserveOriginalsEnabled(enabled: Boolean)
    suspend fun setOrganizeLowConfidenceEnabled(enabled: Boolean)
    suspend fun setMinScreenshotsForSubfolder(min: Int)
}

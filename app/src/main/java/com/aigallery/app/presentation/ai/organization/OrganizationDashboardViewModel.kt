package com.aigallery.app.presentation.ai.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aigallery.app.data.database.CategoryCountResult
import com.aigallery.app.data.database.OrganizationStatsResult
import com.aigallery.app.data.database.SubcategoryCountResult
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.organization.model.ScreenshotCategory
import com.aigallery.app.domain.organization.repository.OrganizationRepository
import com.aigallery.app.domain.organization.repository.OrganizationSettings
import com.aigallery.app.domain.organization.repository.OrganizationSettingsRepository
import com.aigallery.app.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryDisplayItem(
    val category: ScreenshotCategory,
    val count: Int,
    val subcategories: List<String> = emptyList()
)

data class OrganizationDashboardUiState(
    val stats: OrganizationStatsResult = OrganizationStatsResult(),
    val categories: List<CategoryDisplayItem> = emptyList(),
    val isOrganizing: Boolean = false,
    val settings: OrganizationSettings = OrganizationSettings()
)

class OrganizationDashboardViewModel(
    private val organizationRepository: OrganizationRepository,
    private val settingsRepository: OrganizationSettingsRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _isOrganizing = MutableStateFlow(false)
    val isOrganizing: StateFlow<Boolean> = _isOrganizing.asStateFlow()

    init {
        // Automatically sync completed screenshots and classify into smart collections
        viewModelScope.launch(Dispatchers.IO) {
            organizationRepository.syncPendingOrganizeFromIntelligence()
            while (organizationRepository.organizePendingBatch(limit = 50) > 0) {
                // Keep organizing until all understood screenshots are virtually classified
            }
        }
    }

    val uiState: StateFlow<OrganizationDashboardUiState> = combine(
        organizationRepository.getOrganizationStats(),
        organizationRepository.getCategoryCounts(),
        organizationRepository.getSubcategoryCounts(),
        settingsRepository.settingsFlow,
        _isOrganizing
    ) { stats: OrganizationStatsResult, categoryCounts: List<CategoryCountResult>, subcategoryCounts: List<SubcategoryCountResult>, settings: OrganizationSettings, organizing: Boolean ->
        val subcategoryMap = subcategoryCounts.groupBy({ it.category }, { it.subcategory })
        val countMap = categoryCounts.associate { it.category to it.count }

        val displayCategories = ScreenshotCategory.entries.map { cat ->
            CategoryDisplayItem(
                category = cat,
                count = countMap[cat.id] ?: 0,
                subcategories = subcategoryMap[cat.id]?.filterNotNull() ?: emptyList()
            )
        }.sortedWith(
            compareByDescending<CategoryDisplayItem> { it.count }
                .thenBy { it.category.displayName }
        )

        OrganizationDashboardUiState(
            stats = stats,
            categories = displayCategories,
            isOrganizing = organizing || stats.totalPending > 0,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = OrganizationDashboardUiState()
    )

    fun organizeNow() {
        viewModelScope.launch(Dispatchers.IO) {
            _isOrganizing.value = true
            try {
                organizationRepository.syncPendingOrganizeFromIntelligence()
                while (organizationRepository.organizePendingBatch(limit = 50) > 0) {
                }
                organizationRepository.organizeNow()
            } catch (e: Exception) {
                android.util.Log.e("OrgDashboardVM", "Error during organizeNow", e)
            } finally {
                _isOrganizing.value = false
            }
        }
    }

    fun reorganizeAll() {
        viewModelScope.launch(Dispatchers.IO) {
            _isOrganizing.value = true
            try {
                organizationRepository.syncPendingOrganizeFromIntelligence()
                organizationRepository.reorganizeAll()
                while (organizationRepository.organizePendingBatch(limit = 50) > 0) {
                }
            } catch (e: Exception) {
                android.util.Log.e("OrgDashboardVM", "Error during reorganizeAll", e)
            } finally {
                _isOrganizing.value = false
            }
        }
    }

    fun updateSettings(settings: OrganizationSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings)
        }
    }

    fun cleanAllOrganizedCopies() {
        viewModelScope.launch {
            organizationRepository.cleanAllOrganizedCopies()
        }
    }

    fun getCategoryMedia(categoryId: String, subcategory: String? = null): Flow<List<MediaItem>> {
        val organizedFlow = if (subcategory.isNullOrBlank() || subcategory == "All") {
            organizationRepository.getOrganizedMediaByCategory(categoryId)
        } else {
            organizationRepository.getOrganizedMediaBySubcategory(categoryId, subcategory)
        }

        return combine(organizedFlow, mediaRepository.getAllMedia()) { organizedList, allMedia ->
            val mediaMap = allMedia.associateBy { it.id }
            organizedList.mapNotNull { orgItem ->
                mediaMap[orgItem.sourceMediaId]
            }
        }
    }

    fun getSubcategoriesForCategory(categoryId: String): Flow<List<SubcategoryCountResult>> {
        return combine(organizationRepository.getSubcategoryCounts()) { results ->
            results.firstOrNull()?.filter { it.category == categoryId } ?: emptyList()
        }
    }

    class Factory(
        private val organizationRepository: OrganizationRepository,
        private val settingsRepository: OrganizationSettingsRepository,
        private val mediaRepository: MediaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OrganizationDashboardViewModel(
                organizationRepository,
                settingsRepository,
                mediaRepository
            ) as T
        }
    }
}

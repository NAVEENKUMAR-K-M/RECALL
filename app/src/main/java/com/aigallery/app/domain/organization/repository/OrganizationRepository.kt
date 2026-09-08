package com.aigallery.app.domain.organization.repository

import com.aigallery.app.data.database.CategoryCountResult
import com.aigallery.app.data.database.OrganizedMediaEntity
import com.aigallery.app.data.database.OrganizationStatsResult
import com.aigallery.app.data.database.SubcategoryCountResult
import kotlinx.coroutines.flow.Flow

interface OrganizationRepository {

    fun getOrganizationStats(): Flow<OrganizationStatsResult>

    fun getCategoryCounts(): Flow<List<CategoryCountResult>>

    fun getSubcategoryCounts(): Flow<List<SubcategoryCountResult>>

    fun getOrganizedMediaByCategory(category: String): Flow<List<OrganizedMediaEntity>>

    fun getOrganizedMediaBySubcategory(category: String, subcategory: String): Flow<List<OrganizedMediaEntity>>

    fun getOrganizedMedia(sourceMediaId: Long): Flow<OrganizedMediaEntity?>

    suspend fun syncPendingOrganizeFromIntelligence()

    suspend fun organizePendingBatch(limit: Int = 20): Int

    suspend fun organizeNow()

    suspend fun reorganizeAll()

    suspend fun reconcileExternalChanges()

    suspend fun cleanAllOrganizedCopies(): Int
}

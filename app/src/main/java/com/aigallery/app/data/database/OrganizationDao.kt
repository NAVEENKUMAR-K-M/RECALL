package com.aigallery.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface OrganizationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: OrganizedMediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<OrganizedMediaEntity>)

    @Query("SELECT * FROM organized_media WHERE sourceMediaId = :sourceMediaId LIMIT 1")
    suspend fun getOrganizedMedia(sourceMediaId: Long): OrganizedMediaEntity?

    @Query("SELECT * FROM organized_media WHERE sourceMediaId = :sourceMediaId LIMIT 1")
    fun getOrganizedMediaFlow(sourceMediaId: Long): Flow<OrganizedMediaEntity?>

    @Query("SELECT * FROM organized_media ORDER BY updatedAt DESC")
    fun getAllOrganizedMediaFlow(): Flow<List<OrganizedMediaEntity>>

    @Query("SELECT * FROM organized_media WHERE category = :category ORDER BY updatedAt DESC")
    fun getOrganizedMediaByCategoryFlow(category: String): Flow<List<OrganizedMediaEntity>>

    @Query("SELECT * FROM organized_media WHERE category = :category AND subcategory = :subcategory ORDER BY updatedAt DESC")
    fun getOrganizedMediaBySubcategoryFlow(category: String, subcategory: String): Flow<List<OrganizedMediaEntity>>

    @Query("SELECT * FROM organized_media WHERE status = 'PENDING' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getPendingOrganization(limit: Int = 20): List<OrganizedMediaEntity>

    @Query("SELECT category, count(*) as count FROM organized_media WHERE status = 'ORGANIZED' GROUP BY category ORDER BY count DESC")
    fun getOrganizedCategoryCountsFlow(): Flow<List<CategoryCountResult>>

    @Query("SELECT category, subcategory, count(*) as count FROM organized_media WHERE status = 'ORGANIZED' AND subcategory IS NOT NULL GROUP BY category, subcategory ORDER BY count DESC")
    fun getSubcategoryCountsFlow(): Flow<List<SubcategoryCountResult>>

    @Query("SELECT COUNT(*) FROM organized_media WHERE status = 'ORGANIZED'")
    fun getOrganizedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM organized_media WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM organized_media WHERE category = 'OTHER'")
    fun getUnsortedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM organized_media WHERE status = 'STALE'")
    fun getStaleCountFlow(): Flow<Int>

    @Query("UPDATE organized_media SET status = :status, destinationUri = :destinationUri, updatedAt = :updatedAt WHERE sourceMediaId = :sourceMediaId")
    suspend fun updateStatus(
        sourceMediaId: Long,
        status: OrganizationStatus,
        destinationUri: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE organized_media SET status = 'STALE', updatedAt = :updatedAt WHERE sourceMediaId = :sourceMediaId")
    suspend fun markStale(sourceMediaId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE organized_media SET status = 'PENDING', organizationVersion = :newVersion, updatedAt = :updatedAt")
    suspend fun resetAllForReorganization(
        newVersion: Int = 1,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM organized_media WHERE destinationUri IS NOT NULL")
    suspend fun getAllWithDestinationUri(): List<OrganizedMediaEntity>

    @Query("DELETE FROM organized_media WHERE sourceMediaId = :sourceMediaId")
    suspend fun deleteBySourceMediaId(sourceMediaId: Long)

    @Transaction
    @Query("SELECT * FROM organized_media WHERE category = :category ORDER BY updatedAt DESC")
    fun getOrganizedMediaWithDetailsByCategoryFlow(category: String): Flow<List<OrganizedMediaWithDetails>>
}

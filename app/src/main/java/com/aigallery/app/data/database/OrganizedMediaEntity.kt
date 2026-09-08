package com.aigallery.app.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class OrganizationStatus {
    PENDING,
    ORGANIZED,
    FAILED,
    SKIPPED,
    STALE
}

@Entity(
    tableName = "organized_media",
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["mediaId"],
            childColumns = ["sourceMediaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceMediaId"], unique = true),
        Index(value = ["category"]),
        Index(value = ["status"]),
        Index(value = ["organizationVersion"])
    ]
)
data class OrganizedMediaEntity(
    @PrimaryKey
    val sourceMediaId: Long,
    val sourceUri: String,
    val destinationUri: String?,
    val category: String,
    val subcategory: String?,
    val organizationVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: OrganizationStatus = OrganizationStatus.PENDING,
    val confidence: Float = 1.0f,
    val reason: String? = null
)

data class CategoryCountResult(
    val category: String,
    val count: Int
)

data class SubcategoryCountResult(
    val category: String,
    val subcategory: String,
    val count: Int
)

data class OrganizationStatsResult(
    val totalUnderstood: Int = 0,
    val totalOrganized: Int = 0,
    val totalPending: Int = 0,
    val totalUnsorted: Int = 0
)

data class OrganizedMediaWithDetails(
    @Embedded
    val organizedMedia: OrganizedMediaEntity,

    @Relation(
        parentColumn = "sourceMediaId",
        entityColumn = "mediaId",
        entity = ScreenshotEntity::class
    )
    val screenshotWithAI: ScreenshotWithAI?
)

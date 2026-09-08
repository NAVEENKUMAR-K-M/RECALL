package com.aigallery.app.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "screenshot_llm_understandings",
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["screenshotId"], unique = true),
        Index(value = ["intent"]),
        Index(value = ["topic"])
    ]
)
data class ScreenshotLLMUnderstandingEntity(
    @PrimaryKey
    val screenshotId: Long,
    val title: String?,
    val summary: String?,
    val topic: String?,
    val intent: String,
    val importance: String,
    val keywords: String, // Comma-separated list
    val facts: String = "", // Key=Value;Key=Value formatted
    val llmVersion: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "screenshot_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["screenshotId"], unique = true),
        Index(value = ["modelVersion"])
    ]
)
data class ScreenshotEmbeddingEntity(
    @PrimaryKey
    val screenshotId: Long,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val embedding: ByteArray,
    val dimension: Int,
    val modelVersion: String = "local_dense_v1",
    val createdAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ScreenshotEmbeddingEntity
        return screenshotId == other.screenshotId && embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = screenshotId.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}

@Entity(
    tableName = "screenshot_relations",
    primaryKeys = ["sourceId", "targetId"],
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["sourceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["targetId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceId"]),
        Index(value = ["targetId"]),
        Index(value = ["relationType"])
    ]
)
data class ScreenshotRelationEntity(
    val sourceId: Long,
    val targetId: Long,
    val relationType: String,
    val score: Float,
    val reason: String?,
    val modelVersion: String = "v1",
    val createdAt: Long = System.currentTimeMillis()
)

data class RelatedScreenshotItem(
    val screenshotId: Long,
    val mediaId: Long,
    val uri: String,
    val filename: String,
    val relationType: String,
    val score: Float,
    val reason: String?,
    val title: String?
)

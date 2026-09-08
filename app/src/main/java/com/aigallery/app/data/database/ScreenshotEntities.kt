package com.aigallery.app.data.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class ProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

@Entity(
    tableName = "screenshots",
    indices = [
        Index(value = ["mediaId"], unique = true),
        Index(value = ["processingStatus"])
    ]
)
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true)
    val screenshotId: Long = 0,
    val mediaId: Long,
    val uri: String,
    val filename: String,
    val createdAt: Long,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val relativePath: String,
    val processingStatus: ProcessingStatus = ProcessingStatus.PENDING,
    val analyzedAt: Long? = null,
    val modelVersion: Int = 1
)

@Entity(
    tableName = "screenshot_text",
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["screenshotId"], unique = true)]
)
data class ScreenshotTextEntity(
    @PrimaryKey
    val screenshotId: Long,
    val text: String,
    val confidence: Float = 1.0f,
    val language: String? = null,
    val extractedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "screenshot_text_blocks",
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["screenshotId"])]
)
data class TextBlockEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val screenshotId: Long,
    val text: String,
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val confidence: Float = 1.0f
)

@Entity(
    tableName = "screenshot_entities",
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["screenshotId"]),
        Index(value = ["type"]),
        Index(value = ["value"])
    ]
)
data class ScreenshotEntityItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val screenshotId: Long,
    val type: String, // URL, EMAIL, PHONE, DATE, TIME, MONEY, PERCENTAGE, HASHTAG, USERNAME, ORGANIZATION, PERSON, LOCATION, PRODUCT, TOPIC
    val value: String,
    val confidence: Float = 1.0f,
    val source: String = "regex"
)

@Entity(
    tableName = "screenshot_classifications",
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["screenshotId"], unique = true)]
)
data class ScreenshotClassificationEntity(
    @PrimaryKey
    val screenshotId: Long,
    val primaryCategory: String,
    val secondaryCategories: String = "", // Comma-separated
    val confidence: Float = 1.0f
)

@Entity(
    tableName = "screenshot_platforms",
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["screenshotId"], unique = true)]
)
data class ScreenshotPlatformEntity(
    @PrimaryKey
    val screenshotId: Long,
    val platform: String,
    val confidence: Float = 1.0f
)

@Entity(
    tableName = "screenshot_tags",
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["screenshotId"]),
        Index(value = ["tag"])
    ]
)
data class ScreenshotTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val screenshotId: Long,
    val tag: String,
    val confidence: Float = 1.0f,
    val source: String = "ai_derived"
)

@Entity(tableName = "screenshot_collections")
data class ScreenshotCollectionEntity(
    @PrimaryKey
    val collectionId: String,
    val name: String,
    val type: String,
    val description: String = "",
    val confidence: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "screenshot_collection_cross_ref",
    primaryKeys = ["collectionId", "screenshotId"],
    foreignKeys = [
        ForeignKey(
            entity = ScreenshotEntity::class,
            parentColumns = ["screenshotId"],
            childColumns = ["screenshotId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ScreenshotCollectionEntity::class,
            parentColumns = ["collectionId"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["screenshotId"]),
        Index(value = ["collectionId"])
    ]
)
data class ScreenshotCollectionCrossRef(
    val collectionId: String,
    val screenshotId: Long
)

data class ScreenshotWithAI(
    @Embedded
    val screenshot: ScreenshotEntity,

    @Relation(
        parentColumn = "screenshotId",
        entityColumn = "screenshotId"
    )
    val textEntity: ScreenshotTextEntity?,

    @Relation(
        parentColumn = "screenshotId",
        entityColumn = "screenshotId"
    )
    val entities: List<ScreenshotEntityItem> = emptyList(),

    @Relation(
        parentColumn = "screenshotId",
        entityColumn = "screenshotId"
    )
    val classification: ScreenshotClassificationEntity?,

    @Relation(
        parentColumn = "screenshotId",
        entityColumn = "screenshotId"
    )
    val platform: ScreenshotPlatformEntity?,

    @Relation(
        parentColumn = "screenshotId",
        entityColumn = "screenshotId"
    )
    val tags: List<ScreenshotTagEntity> = emptyList(),

    @Relation(
        parentColumn = "screenshotId",
        entityColumn = "screenshotId"
    )
    val llmUnderstanding: ScreenshotLLMUnderstandingEntity? = null,

    @Relation(
        parentColumn = "screenshotId",
        entityColumn = "screenshotId"
    )
    val embedding: ScreenshotEmbeddingEntity? = null
)


package com.aigallery.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenshotDao {

    // --- Screenshot Entity CRUD ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScreenshot(screenshot: ScreenshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertScreenshots(screenshots: List<ScreenshotEntity>): List<Long>

    @Update
    suspend fun updateScreenshot(screenshot: ScreenshotEntity)

    @Query("UPDATE screenshots SET processingStatus = :status, analyzedAt = :analyzedAt WHERE screenshotId = :screenshotId")
    suspend fun updateProcessingStatus(screenshotId: Long, status: ProcessingStatus, analyzedAt: Long? = null)

    @Query("SELECT * FROM screenshots WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getScreenshotByMediaId(mediaId: Long): ScreenshotEntity?

    @Query("SELECT * FROM screenshots WHERE screenshotId = :screenshotId LIMIT 1")
    suspend fun getScreenshotById(screenshotId: Long): ScreenshotEntity?

    @Query("SELECT * FROM screenshots ORDER BY createdAt DESC")
    fun getAllScreenshotsFlow(): Flow<List<ScreenshotEntity>>

    @Query("SELECT * FROM screenshots WHERE processingStatus = 'PENDING' ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getPendingScreenshots(limit: Int = 10): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE processingStatus = 'PENDING' OR processingStatus = 'PROCESSING'")
    suspend fun getUnfinishedScreenshots(): List<ScreenshotEntity>

    @Query("SELECT * FROM screenshots WHERE processingStatus = 'COMPLETED' ORDER BY createdAt DESC")
    suspend fun getCompletedScreenshots(): List<ScreenshotEntity>

    // --- AI Storage Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertText(text: ScreenshotTextEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTextBlocks(blocks: List<TextBlockEntity>)

    @Query("DELETE FROM screenshot_entities WHERE screenshotId = :screenshotId")
    suspend fun deleteEntitiesForScreenshot(screenshotId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntities(entities: List<ScreenshotEntityItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassification(classification: ScreenshotClassificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlatform(platform: ScreenshotPlatformEntity)

    @Query("DELETE FROM screenshot_tags WHERE screenshotId = :screenshotId")
    suspend fun deleteTagsForScreenshot(screenshotId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<ScreenshotTagEntity>)

    @Transaction
    suspend fun saveAnalysisResult(
        screenshotId: Long,
        textEntity: ScreenshotTextEntity?,
        textBlocks: List<TextBlockEntity>,
        entities: List<ScreenshotEntityItem>,
        classification: ScreenshotClassificationEntity?,
        platform: ScreenshotPlatformEntity?,
        tags: List<ScreenshotTagEntity>,
        modelVersion: Int
    ) {
        textEntity?.let { insertText(it) }
        if (textBlocks.isNotEmpty()) {
            insertTextBlocks(textBlocks)
        }
        deleteEntitiesForScreenshot(screenshotId)
        if (entities.isNotEmpty()) {
            insertEntities(entities)
        }
        classification?.let { insertClassification(it) }
        platform?.let { insertPlatform(it) }
        deleteTagsForScreenshot(screenshotId)
        if (tags.isNotEmpty()) {
            insertTags(tags)
        }
        updateProcessingStatus(
            screenshotId = screenshotId,
            status = ProcessingStatus.COMPLETED,
            analyzedAt = System.currentTimeMillis()
        )
    }

    // --- Querying with AI Insights ---

    @Transaction
    @Query("SELECT * FROM screenshots WHERE screenshotId = :screenshotId LIMIT 1")
    fun getScreenshotWithAIFlow(screenshotId: Long): Flow<ScreenshotWithAI?>

    @Transaction
    @Query("SELECT * FROM screenshots WHERE mediaId = :mediaId LIMIT 1")
    fun getScreenshotWithAIByMediaIdFlow(mediaId: Long): Flow<ScreenshotWithAI?>

    @Transaction
    @Query("SELECT * FROM screenshots WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getScreenshotWithAIByMediaIdSync(mediaId: Long): ScreenshotWithAI?

    // --- Smart Search Across All AI Attributes (Lexical + Semantic Metadata) ---

    @Transaction
    @Query("""
        SELECT DISTINCT s.* FROM screenshots s
        LEFT JOIN screenshot_text t ON s.screenshotId = t.screenshotId
        LEFT JOIN screenshot_entities e ON s.screenshotId = e.screenshotId
        LEFT JOIN screenshot_classifications c ON s.screenshotId = c.screenshotId
        LEFT JOIN screenshot_platforms p ON s.screenshotId = p.screenshotId
        LEFT JOIN screenshot_tags tg ON s.screenshotId = tg.screenshotId
        LEFT JOIN screenshot_llm_understandings u ON s.screenshotId = u.screenshotId
        WHERE s.filename LIKE '%' || :query || '%'
           OR t.text LIKE '%' || :query || '%'
           OR e.value LIKE '%' || :query || '%'
           OR c.primaryCategory LIKE '%' || :query || '%'
           OR c.secondaryCategories LIKE '%' || :query || '%'
           OR p.platform LIKE '%' || :query || '%'
           OR tg.tag LIKE '%' || :query || '%'
           OR u.title LIKE '%' || :query || '%'
           OR u.summary LIKE '%' || :query || '%'
           OR u.topic LIKE '%' || :query || '%'
           OR u.intent LIKE '%' || :query || '%'
           OR u.keywords LIKE '%' || :query || '%'
        ORDER BY s.createdAt DESC
    """)
    fun searchScreenshots(query: String): Flow<List<ScreenshotWithAI>>

    @Transaction
    @Query("""
        SELECT DISTINCT s.* FROM screenshots s
        INNER JOIN screenshot_classifications c ON s.screenshotId = c.screenshotId
        WHERE c.primaryCategory = :category OR c.secondaryCategories LIKE '%' || :category || '%'
        ORDER BY s.createdAt DESC
    """)
    fun getScreenshotsByCategory(category: String): Flow<List<ScreenshotWithAI>>

    @Transaction
    @Query("""
        SELECT DISTINCT s.* FROM screenshots s
        INNER JOIN screenshot_platforms p ON s.screenshotId = p.screenshotId
        WHERE p.platform = :platform
        ORDER BY s.createdAt DESC
    """)
    fun getScreenshotsByPlatform(platform: String): Flow<List<ScreenshotWithAI>>

    // --- Phase 4: LLM Understanding Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLLMUnderstanding(understanding: ScreenshotLLMUnderstandingEntity)

    @Query("SELECT * FROM screenshot_llm_understandings WHERE screenshotId = :screenshotId LIMIT 1")
    suspend fun getLLMUnderstandingSync(screenshotId: Long): ScreenshotLLMUnderstandingEntity?

    @Query("SELECT COUNT(*) FROM screenshot_llm_understandings")
    fun getLLMCountFlow(): Flow<Int>

    // --- Phase 4: Local Vector Embeddings Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(embedding: ScreenshotEmbeddingEntity)

    @Query("SELECT * FROM screenshot_embeddings WHERE screenshotId = :screenshotId LIMIT 1")
    suspend fun getEmbeddingSync(screenshotId: Long): ScreenshotEmbeddingEntity?

    @Query("SELECT * FROM screenshot_embeddings")
    suspend fun getAllEmbeddingsSync(): List<ScreenshotEmbeddingEntity>

    @Query("SELECT COUNT(*) FROM screenshot_embeddings")
    fun getEmbeddingCountFlow(): Flow<Int>

    // --- Phase 4: Relationships & AI Memory Graph ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(relation: ScreenshotRelationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelations(relations: List<ScreenshotRelationEntity>)

    @Query("""
        SELECT r.targetId AS screenshotId, s.mediaId, s.uri, s.filename, r.relationType, r.score, r.reason, u.title
        FROM screenshot_relations r
        INNER JOIN screenshots s ON r.targetId = s.screenshotId
        LEFT JOIN screenshot_llm_understandings u ON r.targetId = u.screenshotId
        WHERE r.sourceId = :screenshotId
        ORDER BY r.score DESC
    """)
    fun getRelatedScreenshotsFlow(screenshotId: Long): Flow<List<RelatedScreenshotItem>>

    @Query("SELECT COUNT(*) FROM screenshot_relations")
    fun getRelationsCountFlow(): Flow<Int>

    // --- AI Status & Debug Queries ---

    @Query("SELECT COUNT(*) FROM screenshots")
    fun getTotalCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE processingStatus = 'COMPLETED'")
    fun getCompletedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE processingStatus = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshots WHERE processingStatus = 'FAILED'")
    fun getFailedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshot_text")
    fun getTextCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshot_entities")
    fun getEntityCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM screenshot_tags")
    fun getTagCountFlow(): Flow<Int>

    @Query("UPDATE screenshots SET processingStatus = 'PENDING', analyzedAt = null")
    suspend fun resetAllForReprocessing()

    // --- Smart Virtual Collections ---

    @Query("SELECT * FROM screenshot_collections ORDER BY name ASC")
    fun getAllCollectionsFlow(): Flow<List<ScreenshotCollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: ScreenshotCollectionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollectionCrossRef(crossRef: ScreenshotCollectionCrossRef)

    suspend fun linkScreenshotToCollection(collectionId: String, screenshotId: Long) {
        insertCollectionCrossRef(ScreenshotCollectionCrossRef(collectionId, screenshotId))
    }

    @Transaction
    @Query("""
        SELECT s.* FROM screenshots s
        INNER JOIN screenshot_collection_cross_ref ref ON s.screenshotId = ref.screenshotId
        WHERE ref.collectionId = :collectionId
        ORDER BY s.createdAt DESC
    """)
    fun getScreenshotsForCollectionFlow(collectionId: String): Flow<List<ScreenshotWithAI>>
}


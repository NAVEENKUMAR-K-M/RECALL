package com.aigallery.app.ai

import com.aigallery.app.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * Hardware-agnostic interface for future on-device AI operations.
 * Designed to cleanly target Qualcomm Hexagon NPU / Snapdragon Neural Processing SDK
 * in Phase 2 & 3 without coupling the UI or Gallery data layer.
 */
interface AIEngine {
    /** Whether the on-device AI acceleration runtime (e.g. Qualcomm NPU) is available. */
    fun isAvailable(): Boolean

    /** Initialize model weights and runtime context in background. */
    suspend fun initialize(): Result<Unit>

    /** Full multimodal analysis pass (OCR, object detection, scene classification). */
    suspend fun analyze(mediaItem: MediaItem): Result<MediaAnalysisResult>

    /** Generates dense vector embeddings for semantic vector search. */
    suspend fun generateEmbedding(mediaItem: MediaItem): Result<FloatArray>

    /** Extracts on-device OCR text from documents, receipts, screenshots. */
    suspend fun extractText(mediaItem: MediaItem): Result<String>
}

/**
 * Result representation of on-device multimodal analysis.
 */
data class MediaAnalysisResult(
    val mediaId: Long,
    val tags: List<String> = emptyList(),
    val detectedText: String = "",
    val sceneType: String? = null,
    val faceClusterIds: List<String> = emptyList(),
    val embedding: FloatArray? = null,
    val confidence: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MediaAnalysisResult
        return mediaId == other.mediaId
    }

    override fun hashCode(): Int = mediaId.hashCode()
}

/**
 * Repository interface for storing and querying AI intelligence.
 */
interface MediaAnalysisRepository {
    fun getAnalysis(mediaId: Long): Flow<MediaAnalysisResult?>
    suspend fun saveAnalysis(result: MediaAnalysisResult)
    fun searchByEmbedding(embedding: FloatArray, threshold: Float = 0.7f): Flow<List<Long>>
    fun searchByText(query: String): Flow<List<Long>>
}

/**
 * Conceptual dynamic collections for Phase 4.
 */
enum class AICollectionType(val displayName: String) {
    PEOPLE("People & Pets"),
    PLACES("Places & Travel"),
    DOCUMENTS("Documents & Notes"),
    SCREENSHOTS("Screenshots & Receipts"),
    FOOD("Food & Dining"),
    WORK("Work & Projects")
}

data class AICollection(
    val id: String,
    val type: AICollectionType,
    val title: String,
    val coverMediaId: Long,
    val itemCount: Int
)

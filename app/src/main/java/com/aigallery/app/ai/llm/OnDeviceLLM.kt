package com.aigallery.app.ai.llm

import com.aigallery.app.data.database.ScreenshotEntityItem

data class LLMContext(
    val screenshotId: Long,
    val ocrText: String?,
    val entities: List<ScreenshotEntityItem> = emptyList(),
    val category: String? = null,
    val platform: String? = null,
    val tags: List<String> = emptyList(),
    val filename: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

sealed class LLMResult {
    data class Success(
        val rawResponse: String,
        val understanding: LLMUnderstanding,
        val latencyMs: Long,
        val backend: String
    ) : LLMResult()

    data class Failure(
        val error: String,
        val fallbackUnderstanding: LLMUnderstanding? = null,
        val exception: Throwable? = null
    ) : LLMResult()
}

enum class ModelState {
    NOT_AVAILABLE,
    WEIGHTS_NOT_FOUND,
    LOADING,
    READY,
    ERROR
}

interface OnDeviceLLM {
    suspend fun generate(
        prompt: String,
        context: LLMContext
    ): LLMResult

    fun getModelState(): ModelState
    fun getBackendName(): String
    fun getModelName(): String
}

interface LocalModelManager {
    suspend fun loadLLM(): Result<Unit>
    suspend fun unloadLLM()
    fun isLoaded(): Boolean
    fun getModelState(): ModelState
}

package com.aigallery.app.data.worker

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.aigallery.app.ai.ScreenshotAIEngineImpl
import com.aigallery.app.ai.embedding.LocalSemanticEmbeddingModel
import com.aigallery.app.ai.llm.DeterministicSemanticReasoner
import com.aigallery.app.ai.llm.ImportanceLevel
import com.aigallery.app.ai.llm.LLMContext
import com.aigallery.app.ai.llm.LLMPromptBuilderImpl
import com.aigallery.app.ai.llm.LLMResult
import com.aigallery.app.ai.llm.LLMUnderstanding
import com.aigallery.app.ai.llm.LocalOnDeviceLLM
import com.aigallery.app.ai.llm.ScreenshotIntent
import com.aigallery.app.ai.relationship.ScreenshotRelationshipEngineImpl
import com.aigallery.app.core.util.BitmapUtils
import com.aigallery.app.data.database.AppDatabase
import com.aigallery.app.data.database.ProcessingStatus
import com.aigallery.app.data.database.ScreenshotCollectionCrossRef
import com.aigallery.app.data.database.ScreenshotCollectionEntity
import com.aigallery.app.data.database.ScreenshotEmbeddingEntity
import com.aigallery.app.data.database.ScreenshotLLMUnderstandingEntity
import com.aigallery.app.data.database.ScreenshotRelationEntity
import com.aigallery.app.data.database.ScreenshotTextEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenshotProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val database = AppDatabase.getInstance(appContext)
    private val screenshotDao = database.screenshotDao()
    private val aiEngine = ScreenshotAIEngineImpl()
    private val onDeviceLLM = com.aigallery.app.ai.AIModelRegistry.getLLMProvider(appContext)
    private val promptBuilder = LLMPromptBuilderImpl()
    private val embeddingModel = LocalSemanticEmbeddingModel()
    private val relationshipEngine = ScreenshotRelationshipEngineImpl(embeddingModel)


    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        try {
            var pendingBatch = screenshotDao.getPendingScreenshots(limit = 15)

            while (pendingBatch.isNotEmpty() && !isStopped) {
                for (screenshot in pendingBatch) {
                    if (isStopped) {
                        return@withContext Result.retry()
                    }

                    // Mark processing status
                    screenshotDao.updateProcessingStatus(
                        screenshotId = screenshot.screenshotId,
                        status = ProcessingStatus.PROCESSING
                    )

                    android.util.Log.i("ScreenshotWorker", "Processing screenshot ${screenshot.screenshotId}: ${screenshot.filename} (${screenshot.uri})")

                    try {
                        val uri = Uri.parse(screenshot.uri)
                        val bitmap = BitmapUtils.decodeSampledBitmapFromUri(
                            context = applicationContext,
                            uri = uri,
                            maxDimension = 1280
                        )

                        if (bitmap == null) {
                            android.util.Log.w("ScreenshotWorker", "Bitmap decode failed for ${screenshot.uri}")
                            screenshotDao.updateProcessingStatus(
                                screenshotId = screenshot.screenshotId,
                                status = ProcessingStatus.FAILED
                            )
                            continue
                        }

                        // Run the complete on-device AI analysis pipeline (Phase 2)
                        val analysis = aiEngine.analyze(
                            screenshotId = screenshot.screenshotId,
                            bitmap = bitmap
                        )

                        // Recycle bitmap immediately to release native memory
                        bitmap.recycle()

                        // Build Room text entity
                        val textEntity = ScreenshotTextEntity(
                            screenshotId = screenshot.screenshotId,
                            text = analysis.ocrText,
                            confidence = analysis.overallConfidence,
                            language = analysis.language,
                            extractedAt = System.currentTimeMillis()
                        )

                        // Save all structured AI data in a single Room database transaction
                        screenshotDao.saveAnalysisResult(
                            screenshotId = screenshot.screenshotId,
                            textEntity = textEntity,
                            textBlocks = analysis.textBlocks,
                            entities = analysis.entities,
                            classification = analysis.classification,
                            platform = analysis.platform,
                            tags = analysis.tags,
                            modelVersion = screenshot.modelVersion
                        )

                        // --- Phase 4: On-Device LLM Understanding ---
                        val llmContext = LLMContext(
                            screenshotId = screenshot.screenshotId,
                            ocrText = analysis.ocrText,
                            entities = analysis.entities,
                            category = analysis.classification?.primaryCategory,
                            platform = analysis.platform?.platform,
                            tags = analysis.tags.map { it.tag },
                            filename = screenshot.filename,
                            createdAt = screenshot.createdAt
                        )

                        val prompt = promptBuilder.buildScreenshotUnderstandingPrompt(llmContext)
                        val llmResult = onDeviceLLM.generate(prompt, llmContext)
                        val understanding = when (llmResult) {
                            is LLMResult.Success -> llmResult.understanding
                            is LLMResult.Failure -> llmResult.fallbackUnderstanding ?: DeterministicSemanticReasoner.reason(llmContext)
                        }

                        val factsString = understanding.facts.entries.joinToString(";") { "${it.key}=${it.value}" }
                        val keywordsString = understanding.keywords.joinToString(",")

                        val llmEntity = ScreenshotLLMUnderstandingEntity(
                            screenshotId = screenshot.screenshotId,
                            title = understanding.title,
                            summary = understanding.summary,
                            topic = understanding.topic,
                            intent = understanding.intent.name,
                            importance = understanding.importance.name,
                            keywords = keywordsString,
                            facts = factsString,
                            llmVersion = 1,
                            createdAt = System.currentTimeMillis()
                        )
                        screenshotDao.insertLLMUnderstanding(llmEntity)

                        // --- Phase 4: Generate Dense Semantic Vector Embedding ---
                        val allTokens = mutableListOf<String>()
                        understanding.keywords.forEach { allTokens.add(it) }
                        understanding.topic?.let { allTokens.add(it) }
                        analysis.classification?.primaryCategory?.let { allTokens.add(it) }
                        analysis.entities.forEach { allTokens.add(it.value) }

                        val embeddingVector = embeddingModel.generateEmbedding(
                            text = "${understanding.title.orEmpty()} ${understanding.summary.orEmpty()} ${analysis.ocrText.take(500)}",
                            tokens = allTokens
                        )
                        val embeddingBytes = embeddingModel.serialize(embeddingVector)
                        val embeddingEntity = ScreenshotEmbeddingEntity(
                            screenshotId = screenshot.screenshotId,
                            embedding = embeddingBytes,
                            dimension = embeddingModel.getDimension(),
                            modelVersion = embeddingModel.getModelVersion(),
                            createdAt = System.currentTimeMillis()
                        )
                        screenshotDao.insertEmbedding(embeddingEntity)

                        // --- Phase 4: Relationship Detection & AI Memory Graph ---
                        val currentWithAI = screenshotDao.getScreenshotWithAIByMediaIdSync(screenshot.mediaId)
                        if (currentWithAI != null) {
                            val completedScreenshots = screenshotDao.getCompletedScreenshots().take(25)
                            val discoveredRelations = mutableListOf<ScreenshotRelationEntity>()

                            for (other in completedScreenshots) {
                                if (other.screenshotId == screenshot.screenshotId) continue
                                val otherWithAI = screenshotDao.getScreenshotWithAIByMediaIdSync(other.mediaId) ?: continue
                                val otherUnderstandingEntity = screenshotDao.getLLMUnderstandingSync(other.screenshotId)
                                val otherUnderstanding = otherUnderstandingEntity?.let {
                                    LLMUnderstanding(
                                        title = it.title,
                                        summary = it.summary,
                                        topic = it.topic,
                                        intent = ScreenshotIntent.fromString(it.intent),
                                        importance = ImportanceLevel.fromString(it.importance),
                                        keywords = it.keywords.split(",").filter { k -> k.isNotBlank() }
                                    )
                                }
                                val otherEmbeddingEntity = screenshotDao.getEmbeddingSync(other.screenshotId)
                                val otherEmbedding = otherEmbeddingEntity?.let { embeddingModel.deserialize(it.embedding) }

                                val relation = relationshipEngine.evaluateRelationship(
                                    source = currentWithAI,
                                    sourceUnderstanding = understanding,
                                    sourceEmbedding = embeddingVector,
                                    candidate = otherWithAI,
                                    candidateUnderstanding = otherUnderstanding,
                                    candidateEmbedding = otherEmbedding
                                )

                                if (relation != null) {
                                    discoveredRelations.add(
                                        ScreenshotRelationEntity(
                                            sourceId = relation.sourceId,
                                            targetId = relation.targetId,
                                            relationType = relation.relationType.name,
                                            score = relation.score,
                                            reason = relation.reason,
                                            modelVersion = "v1"
                                        )
                                    )
                                    discoveredRelations.add(
                                        ScreenshotRelationEntity(
                                            sourceId = relation.targetId,
                                            targetId = relation.sourceId,
                                            relationType = relation.relationType.name,
                                            score = relation.score,
                                            reason = relation.reason,
                                            modelVersion = "v1"
                                        )
                                    )
                                }
                            }

                            if (discoveredRelations.isNotEmpty()) {
                                screenshotDao.insertRelations(discoveredRelations)
                            }
                        }

                        android.util.Log.i(
                            "ScreenshotWorker",
                            "PHASE 4 SUCCESS: ID=${screenshot.screenshotId}, Title=${understanding.title}, " +
                            "Intent=${understanding.intent}, Topic=${understanding.topic}, Relations evaluated."
                        )

                        // Link to Smart Collections if applicable
                        analysis.classification?.let { cls ->
                            if (cls.primaryCategory != "OTHER") {
                                val collectionId = "col_${cls.primaryCategory.lowercase()}"
                                val collectionName = cls.primaryCategory.lowercase()
                                    .replaceFirstChar { it.titlecase() }
                                
                                screenshotDao.insertCollection(
                                    ScreenshotCollectionEntity(
                                        collectionId = collectionId,
                                        name = collectionName,
                                        type = "CATEGORY",
                                        description = "Smart collection for $collectionName screenshots",
                                        confidence = cls.confidence
                                    )
                                )
                                screenshotDao.insertCollectionCrossRef(
                                    ScreenshotCollectionCrossRef(
                                        collectionId = collectionId,
                                        screenshotId = screenshot.screenshotId
                                    )
                                )
                            }
                        }

                    } catch (e: Exception) {
                        android.util.Log.e("ScreenshotWorker", "Error processing screenshot ${screenshot.screenshotId}", e)
                        screenshotDao.updateProcessingStatus(
                            screenshotId = screenshot.screenshotId,
                            status = ProcessingStatus.FAILED
                        )
                    }
                }

                // Fetch next batch
                pendingBatch = screenshotDao.getPendingScreenshots(limit = 15)
            }


            // Trigger automatic organization worker so screenshots are categorized into smart collections
            ScreenshotOrganizationWorker.enqueue(applicationContext)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "screenshot_ai_processing_worker"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<ScreenshotProcessingWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}

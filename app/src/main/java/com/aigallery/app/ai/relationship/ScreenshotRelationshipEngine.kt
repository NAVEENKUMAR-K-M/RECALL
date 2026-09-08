package com.aigallery.app.ai.relationship

import com.aigallery.app.ai.embedding.EmbeddingModel
import com.aigallery.app.ai.llm.LLMUnderstanding
import com.aigallery.app.data.database.ScreenshotWithAI
import kotlin.math.abs

enum class RelationType(val displayName: String) {
    SAME_TRIP("Same Trip"),
    SAME_TOPIC("Same Topic"),
    SAME_PURCHASE("Same Purchase"),
    SAME_EVENT("Same Event"),
    SAME_LOCATION("Same Location"),
    RELATED("Related");
}

data class DiscoveredRelation(
    val sourceId: Long,
    val targetId: Long,
    val relationType: RelationType,
    val score: Float,
    val reason: String
)

interface ScreenshotRelationshipEngine {
    fun evaluateRelationship(
        source: ScreenshotWithAI,
        sourceUnderstanding: LLMUnderstanding?,
        sourceEmbedding: FloatArray?,
        candidate: ScreenshotWithAI,
        candidateUnderstanding: LLMUnderstanding?,
        candidateEmbedding: FloatArray?
    ): DiscoveredRelation?
}

class ScreenshotRelationshipEngineImpl(
    private val embeddingModel: EmbeddingModel
) : ScreenshotRelationshipEngine {

    override fun evaluateRelationship(
        source: ScreenshotWithAI,
        sourceUnderstanding: LLMUnderstanding?,
        sourceEmbedding: FloatArray?,
        candidate: ScreenshotWithAI,
        candidateUnderstanding: LLMUnderstanding?,
        candidateEmbedding: FloatArray?
    ): DiscoveredRelation? {
        if (source.screenshot.screenshotId == candidate.screenshot.screenshotId) return null

        val sourceEntities = source.entities.map { it.type to it.value.lowercase().trim() }
        val candEntities = candidate.entities.map { it.type to it.value.lowercase().trim() }

        val sourceLoc = sourceEntities.firstOrNull { it.first == "LOCATION" }?.second
        val candLoc = candEntities.firstOrNull { it.first == "LOCATION" }?.second

        val sourceCat = source.classification?.primaryCategory.orEmpty()
        val candCat = candidate.classification?.primaryCategory.orEmpty()

        val sourceOrg = sourceEntities.firstOrNull { it.first == "ORGANIZATION" }?.second
        val candOrg = candEntities.firstOrNull { it.first == "ORGANIZATION" }?.second

        // 1. SAME_TRIP Check
        if (sourceLoc != null && candLoc != null && sourceLoc == candLoc) {
            if (sourceCat == "TRAVEL" || candCat == "TRAVEL" ||
                sourceUnderstanding?.topic?.contains("trip", ignoreCase = true) == true ||
                candidateUnderstanding?.topic?.contains("trip", ignoreCase = true) == true

            ) {
                return DiscoveredRelation(
                    sourceId = source.screenshot.screenshotId,
                    targetId = candidate.screenshot.screenshotId,
                    relationType = RelationType.SAME_TRIP,
                    score = 0.95f,
                    reason = "Both relate to travel in ${sourceLoc.replaceFirstChar { it.uppercase() }}"
                )
            }
        }

        // 2. SAME_TOPIC Check (e.g. Google Internship or shared Organization)
        if (sourceOrg != null && candOrg != null && sourceOrg == candOrg) {
            val sTopic = sourceUnderstanding?.topic?.lowercase().orEmpty()
            val cTopic = candidateUnderstanding?.topic?.lowercase().orEmpty()
            if (sTopic.isNotBlank() && cTopic.isNotBlank() && (sTopic == cTopic || sTopic.contains("intern") && cTopic.contains("intern"))) {
                return DiscoveredRelation(
                    sourceId = source.screenshot.screenshotId,
                    targetId = candidate.screenshot.screenshotId,
                    relationType = RelationType.SAME_TOPIC,
                    score = 0.90f,
                    reason = "Shared topic '${sourceUnderstanding?.topic}' at ${sourceOrg.replaceFirstChar { it.uppercase() }}"
                )
            }
        }

        // 3. SAME_PURCHASE Check
        val sourceMoney = sourceEntities.firstOrNull { it.first == "MONEY" }?.second
        val candMoney = candEntities.firstOrNull { it.first == "MONEY" }?.second
        if (sourceCat == "SHOPPING" && candCat == "SHOPPING" && sourceMoney != null && candMoney != null && sourceMoney == candMoney) {
            return DiscoveredRelation(
                sourceId = source.screenshot.screenshotId,
                targetId = candidate.screenshot.screenshotId,
                relationType = RelationType.SAME_PURCHASE,
                score = 0.85f,
                reason = "Matching purchase amount $sourceMoney"
            )
        }

        // 4. SAME_EVENT Check (Temporal proximity + tickets)
        val timeDiff = abs(source.screenshot.createdAt - candidate.screenshot.createdAt)
        val isSameDay = timeDiff < 24 * 3600 * 1000L
        if (isSameDay && sourceCat == "TICKETS" && candCat == "TICKETS") {
            return DiscoveredRelation(
                sourceId = source.screenshot.screenshotId,
                targetId = candidate.screenshot.screenshotId,
                relationType = RelationType.SAME_EVENT,
                score = 0.80f,
                reason = "Event tickets captured around the same time"
            )
        }

        // 5. Semantic Vector Cosine Similarity
        if (sourceEmbedding != null && candidateEmbedding != null) {
            val similarity = embeddingModel.cosineSimilarity(sourceEmbedding, candidateEmbedding)
            if (similarity >= 0.72f) {
                return DiscoveredRelation(
                    sourceId = source.screenshot.screenshotId,
                    targetId = candidate.screenshot.screenshotId,
                    relationType = RelationType.RELATED,
                    score = similarity,
                    reason = "High semantic content similarity (${(similarity * 100).toInt()}%)"
                )
            }
        }

        return null
    }
}

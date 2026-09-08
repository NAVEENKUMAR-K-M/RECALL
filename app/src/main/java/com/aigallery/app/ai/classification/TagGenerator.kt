package com.aigallery.app.ai.classification

import com.aigallery.app.data.database.ScreenshotClassificationEntity
import com.aigallery.app.data.database.ScreenshotEntityItem
import com.aigallery.app.data.database.ScreenshotPlatformEntity
import com.aigallery.app.data.database.ScreenshotTagEntity
import java.util.Locale

object TagGenerator {

    private val TRAVEL_DESTINATIONS = listOf(
        "Goa", "Mumbai", "Delhi", "Bengaluru", "Bangalore", "Hyderabad",
        "Chennai", "Kolkata", "Pune", "Jaipur", "Kerala", "Manali",
        "Shimla", "Dubai", "Singapore", "London", "Paris", "New York"
    )

    fun generate(
        screenshotId: Long,
        entities: List<ScreenshotEntityItem>,
        classification: ScreenshotClassificationEntity?,
        platform: ScreenshotPlatformEntity?,
        fullText: String
    ): List<ScreenshotTagEntity> {
        val tags = mutableListOf<ScreenshotTagEntity>()
        val seen = mutableSetOf<String>()

        fun addTag(tag: String, confidence: Float, source: String) {
            val clean = tag.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            if (clean.length in 2..40 && seen.add(clean.lowercase(Locale.ROOT))) {
                tags.add(
                    ScreenshotTagEntity(
                        screenshotId = screenshotId,
                        tag = clean,
                        confidence = confidence,
                        source = source
                    )
                )
            }
        }

        // 1. Tags from Organizations
        for (entity in entities.filter { it.type == "ORGANIZATION" }) {
            addTag(entity.value, entity.confidence, "entity_org")
        }

        // 2. Tags from Topics
        for (entity in entities.filter { it.type == "TOPIC" }) {
            addTag(entity.value, entity.confidence, "entity_topic")
        }

        // 3. Tags from Hashtags
        for (entity in entities.filter { it.type == "HASHTAG" }) {
            val hashClean = entity.value.removePrefix("#")
            addTag(hashClean, entity.confidence, "hashtag")
        }

        // 4. Tags from Platform
        platform?.let {
            addTag(it.platform, it.confidence, "platform")
        }

        // 5. Tags from Classification
        classification?.let {
            if (it.primaryCategory != ScreenshotClassifier.Categories.OTHER) {
                addTag(it.primaryCategory.lowercase().replaceFirstChar { c -> c.titlecase() }, it.confidence, "category")
            }
            it.secondaryCategories.split(",").filter { cat -> cat.isNotBlank() }.forEach { sec ->
                addTag(sec.lowercase().replaceFirstChar { c -> c.titlecase() }, 0.80f, "sub_category")
            }
        }

        // 6. Travel destination matches
        for (dest in TRAVEL_DESTINATIONS) {
            val regex = Regex("\\b${Regex.escape(dest)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(fullText)) {
                addTag(dest, 0.90f, "destination")
            }
        }

        return tags.take(12)
    }
}

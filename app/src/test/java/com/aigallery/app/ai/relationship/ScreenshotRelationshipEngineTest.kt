package com.aigallery.app.ai.relationship

import com.aigallery.app.ai.embedding.LocalSemanticEmbeddingModel
import com.aigallery.app.ai.llm.ImportanceLevel
import com.aigallery.app.ai.llm.LLMUnderstanding
import com.aigallery.app.ai.llm.ScreenshotIntent
import com.aigallery.app.data.database.ScreenshotClassificationEntity
import com.aigallery.app.data.database.ScreenshotEntity
import com.aigallery.app.data.database.ScreenshotEntityItem
import com.aigallery.app.data.database.ScreenshotWithAI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenshotRelationshipEngineTest {

    private val embeddingModel = LocalSemanticEmbeddingModel()
    private val engine = ScreenshotRelationshipEngineImpl(embeddingModel)

    @Test
    fun testSameTripDetection() {
        val hotelScreenshot = ScreenshotWithAI(
            screenshot = ScreenshotEntity(screenshotId = 1L, mediaId = 101L, uri = "uri1", filename = "hotel.png", createdAt = 1000L, width = 1080, height = 2400, fileSize = 1000, relativePath = "Pictures/Screenshots"),
            textEntity = null,
            entities = listOf(
                ScreenshotEntityItem(id = 1, screenshotId = 1L, type = "LOCATION", value = "Goa"),
                ScreenshotEntityItem(id = 2, screenshotId = 1L, type = "ORGANIZATION", value = "Taj Exotica")
            ),
            classification = ScreenshotClassificationEntity(screenshotId = 1L, primaryCategory = "TRAVEL", secondaryCategories = "HOTEL", confidence = 0.95f),
            platform = null,
            tags = emptyList()
        )

        val flightScreenshot = ScreenshotWithAI(
            screenshot = ScreenshotEntity(screenshotId = 2L, mediaId = 102L, uri = "uri2", filename = "flight.png", createdAt = 2000L, width = 1080, height = 2400, fileSize = 1000, relativePath = "Pictures/Screenshots"),
            textEntity = null,
            entities = listOf(
                ScreenshotEntityItem(id = 3, screenshotId = 2L, type = "LOCATION", value = "Goa"),
                ScreenshotEntityItem(id = 4, screenshotId = 2L, type = "ORGANIZATION", value = "IndiGo")
            ),
            classification = ScreenshotClassificationEntity(screenshotId = 2L, primaryCategory = "TRAVEL", secondaryCategories = "FLIGHT", confidence = 0.95f),
            platform = null,
            tags = emptyList()
        )

        val relation = engine.evaluateRelationship(
            source = hotelScreenshot,
            sourceUnderstanding = LLMUnderstanding(title = "Goa Hotel Booking", summary = "Hotel in Goa", topic = "Goa Trip", intent = ScreenshotIntent.BOOKING),
            sourceEmbedding = embeddingModel.generateEmbedding("Taj Exotica Goa Hotel"),
            candidate = flightScreenshot,
            candidateUnderstanding = LLMUnderstanding(title = "Goa Flight Booking", summary = "IndiGo Flight to Goa", topic = "Goa Trip", intent = ScreenshotIntent.BOOKING),
            candidateEmbedding = embeddingModel.generateEmbedding("IndiGo Flight to Goa")
        )

        assertNotNull(relation)
        assertEquals(RelationType.SAME_TRIP, relation?.relationType)
    }

    @Test
    fun testSameTopicDetection() {
        val offerScreenshot = ScreenshotWithAI(
            screenshot = ScreenshotEntity(screenshotId = 3L, mediaId = 103L, uri = "uri3", filename = "offer.png", createdAt = 1000L, width = 1080, height = 2400, fileSize = 1000, relativePath = "Pictures/Screenshots"),
            textEntity = null,
            entities = listOf(
                ScreenshotEntityItem(id = 5, screenshotId = 3L, type = "ORGANIZATION", value = "Google"),
                ScreenshotEntityItem(id = 6, screenshotId = 3L, type = "TOPIC", value = "Internship")
            ),
            classification = ScreenshotClassificationEntity(screenshotId = 3L, primaryCategory = "WORK", secondaryCategories = "INTERNSHIP", confidence = 0.95f),
            platform = null,
            tags = emptyList()
        )

        val jdScreenshot = ScreenshotWithAI(
            screenshot = ScreenshotEntity(screenshotId = 4L, mediaId = 104L, uri = "uri4", filename = "jd.png", createdAt = 2000L, width = 1080, height = 2400, fileSize = 1000, relativePath = "Pictures/Screenshots"),
            textEntity = null,
            entities = listOf(
                ScreenshotEntityItem(id = 7, screenshotId = 4L, type = "ORGANIZATION", value = "Google"),
                ScreenshotEntityItem(id = 8, screenshotId = 4L, type = "TOPIC", value = "Internship")
            ),
            classification = ScreenshotClassificationEntity(screenshotId = 4L, primaryCategory = "WORK", secondaryCategories = "CAREERS", confidence = 0.95f),
            platform = null,
            tags = emptyList()
        )

        val relation = engine.evaluateRelationship(
            source = offerScreenshot,
            sourceUnderstanding = LLMUnderstanding(title = "Google Offer Letter", summary = "Offer", topic = "Internship", intent = ScreenshotIntent.WORK),
            sourceEmbedding = null,
            candidate = jdScreenshot,
            candidateUnderstanding = LLMUnderstanding(title = "Google Job Description", summary = "JD", topic = "Internship", intent = ScreenshotIntent.WORK),
            candidateEmbedding = null
        )

        assertNotNull(relation)
        assertEquals(RelationType.SAME_TOPIC, relation?.relationType)
    }

    @Test
    fun testUnrelatedScreenshotsReturnNull() {
        val travelScreenshot = ScreenshotWithAI(
            screenshot = ScreenshotEntity(screenshotId = 5L, mediaId = 105L, uri = "uri5", filename = "travel.png", createdAt = 1000L, width = 1080, height = 2400, fileSize = 1000, relativePath = "Pictures/Screenshots"),
            textEntity = null,
            entities = listOf(ScreenshotEntityItem(id = 9, screenshotId = 5L, type = "LOCATION", value = "Goa")),
            classification = ScreenshotClassificationEntity(screenshotId = 5L, primaryCategory = "TRAVEL", secondaryCategories = "", confidence = 0.95f),
            platform = null,
            tags = emptyList()
        )

        val mathScreenshot = ScreenshotWithAI(
            screenshot = ScreenshotEntity(screenshotId = 6L, mediaId = 106L, uri = "uri6", filename = "math.png", createdAt = 50000000L, width = 1080, height = 2400, fileSize = 1000, relativePath = "Pictures/Screenshots"),
            textEntity = null,
            entities = listOf(ScreenshotEntityItem(id = 10, screenshotId = 6L, type = "TOPIC", value = "Calculus")),
            classification = ScreenshotClassificationEntity(screenshotId = 6L, primaryCategory = "EDUCATION", secondaryCategories = "", confidence = 0.95f),
            platform = null,
            tags = emptyList()
        )

        val relation = engine.evaluateRelationship(
            source = travelScreenshot,
            sourceUnderstanding = LLMUnderstanding(title = "Goa Beach", summary = "Beach", topic = "Travel", intent = ScreenshotIntent.TRAVEL),
            sourceEmbedding = embeddingModel.generateEmbedding("Goa Beach Sunset"),
            candidate = mathScreenshot,
            candidateUnderstanding = LLMUnderstanding(title = "Calculus Derivatives", summary = "Math", topic = "Math", intent = ScreenshotIntent.EDUCATION),
            candidateEmbedding = embeddingModel.generateEmbedding("Calculus Differentiation Derivatives")
        )

        assertNull(relation)
    }
}

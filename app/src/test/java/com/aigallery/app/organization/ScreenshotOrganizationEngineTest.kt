package com.aigallery.app.organization

import com.aigallery.app.ai.organization.ScreenshotOrganizationEngineImpl
import com.aigallery.app.data.database.ProcessingStatus
import com.aigallery.app.data.database.ScreenshotClassificationEntity
import com.aigallery.app.data.database.ScreenshotEntity
import com.aigallery.app.data.database.ScreenshotEntityItem
import com.aigallery.app.data.database.ScreenshotPlatformEntity
import com.aigallery.app.data.database.ScreenshotTagEntity
import com.aigallery.app.data.database.ScreenshotTextEntity
import com.aigallery.app.data.database.ScreenshotWithAI
import com.aigallery.app.domain.organization.model.ScreenshotCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScreenshotOrganizationEngineTest {

    private lateinit var engine: ScreenshotOrganizationEngineImpl

    @Before
    fun setUp() {
        engine = ScreenshotOrganizationEngineImpl()
    }

    private fun createScreenshotWithAI(
        primaryCategory: String = "OTHER",
        secondaryCategories: String = "",
        confidence: Float = 0.9f,
        platform: String? = null,
        entities: List<Pair<String, String>> = emptyList(),
        tags: List<String> = emptyList(),
        ocrText: String = ""
    ): ScreenshotWithAI {
        val screenshot = ScreenshotEntity(
            screenshotId = 101L,
            mediaId = 1001L,
            uri = "content://media/external/images/media/1001",
            filename = "Screenshot_20260908.png",
            createdAt = System.currentTimeMillis(),
            width = 1080,
            height = 2400,
            fileSize = 1024000L,
            relativePath = "Pictures/Screenshots/",
            processingStatus = ProcessingStatus.COMPLETED
        )

        return ScreenshotWithAI(
            screenshot = screenshot,
            textEntity = ScreenshotTextEntity(screenshotId = 101L, text = ocrText),
            entities = entities.map { (type, value) ->
                ScreenshotEntityItem(screenshotId = 101L, type = type, value = value)
            },
            classification = ScreenshotClassificationEntity(
                screenshotId = 101L,
                primaryCategory = primaryCategory,
                secondaryCategories = secondaryCategories,
                confidence = confidence
            ),
            platform = platform?.let { ScreenshotPlatformEntity(screenshotId = 101L, platform = it) },
            tags = tags.map { ScreenshotTagEntity(screenshotId = 101L, tag = it) }
        )
    }

    @Test
    fun testAmazonScreenshot_mapsToShopping() = runTest {
        val analysis = createScreenshotWithAI(
            primaryCategory = "SHOPPING",
            platform = "Amazon",
            entities = listOf("ORGANIZATION" to "Amazon", "MONEY" to "₹1,499"),
            tags = listOf("shopping", "order", "delivery")
        )

        val result = engine.organize(analysis.screenshot, analysis)

        assertEquals(ScreenshotCategory.SHOPPING, result.primaryCategory)
        assertEquals("Amazon", result.subcategory)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test
    fun testUpiPayment_mapsToFinance() = runTest {
        val analysis = createScreenshotWithAI(
            primaryCategory = "FINANCE",
            platform = "PhonePe",
            entities = listOf("MONEY" to "₹500", "ORGANIZATION" to "UPI"),
            tags = listOf("payment", "successful", "transaction")
        )

        val result = engine.organize(analysis.screenshot, analysis)

        assertEquals(ScreenshotCategory.FINANCE, result.primaryCategory)
        assertTrue(result.confidence >= 0.8f)
    }

    @Test
    fun testFlightTicket_mapsToTravelAndTickets() = runTest {
        val analysis = createScreenshotWithAI(
            primaryCategory = "TRAVEL",
            secondaryCategories = "TICKETS",
            entities = listOf("LOCATION" to "Goa", "DATE" to "18 Aug 2026"),
            tags = listOf("flight", "boarding-pass", "travel")
        )

        val result = engine.organize(analysis.screenshot, analysis)

        assertEquals(ScreenshotCategory.TRAVEL, result.primaryCategory)
        assertEquals("Goa", result.subcategory)
        assertTrue(result.secondaryCategories.contains(ScreenshotCategory.TICKETS))
    }

    @Test
    fun testInstagramScreenshot_mapsToSocialMedia() = runTest {
        val analysis = createScreenshotWithAI(
            primaryCategory = "SOCIAL_MEDIA",
            platform = "Instagram",
            entities = listOf("TOPIC" to "Reels"),
            tags = listOf("instagram", "post", "social")
        )

        val result = engine.organize(analysis.screenshot, analysis)

        assertEquals(ScreenshotCategory.SOCIAL_MEDIA, result.primaryCategory)
        assertEquals("Instagram", result.subcategory)
    }

    @Test
    fun testCollegeNotes_mapsToEducation() = runTest {
        val analysis = createScreenshotWithAI(
            primaryCategory = "EDUCATION",
            entities = listOf("TOPIC" to "Operating Systems"),
            tags = listOf("lecture", "exam", "notes")
        )

        val result = engine.organize(analysis.screenshot, analysis)

        assertEquals(ScreenshotCategory.EDUCATION, result.primaryCategory)
    }

    @Test
    fun testCodingScreenshot_mapsToTechnology() = runTest {
        val analysis = createScreenshotWithAI(
            primaryCategory = "TECHNOLOGY",
            platform = "GitHub",
            entities = listOf("ORGANIZATION" to "Nvidia", "TOPIC" to "CUDA"),
            tags = listOf("code", "programming", "python")
        )

        val result = engine.organize(analysis.screenshot, analysis)

        assertEquals(ScreenshotCategory.TECHNOLOGY, result.primaryCategory)
    }

    @Test
    fun testUnknownScreenshot_mapsToOther() = runTest {
        val analysis = createScreenshotWithAI(
            primaryCategory = "OTHER",
            confidence = 0.2f
        )

        val result = engine.organize(analysis.screenshot, analysis)

        assertEquals(ScreenshotCategory.OTHER, result.primaryCategory)
    }

    @Test
    fun testConfidenceThreshold_lowConfidenceExcludedByDefault() = runTest {
        val analysis = createScreenshotWithAI(
            primaryCategory = "SHOPPING",
            confidence = 0.4f // Below default 0.65 threshold
        )

        val result = engine.organize(
            screenshot = analysis.screenshot,
            analysis = analysis,
            organizeLowConfidence = false
        )

        assertEquals(ScreenshotCategory.OTHER, result.primaryCategory)
    }

    @Test
    fun testConfidenceThreshold_lowConfidenceIncludedWhenEnabled() = runTest {
        val analysis = createScreenshotWithAI(
            primaryCategory = "SHOPPING",
            confidence = 0.4f
        )

        val result = engine.organize(
            screenshot = analysis.screenshot,
            analysis = analysis,
            organizeLowConfidence = true
        )

        assertEquals(ScreenshotCategory.SHOPPING, result.primaryCategory)
    }

    @Test
    fun testSubcategoryGeneration_cleanFormatting() = runTest {
        val analysis = createScreenshotWithAI(
            primaryCategory = "TRAVEL",
            entities = listOf("LOCATION" to "goa trip")
        )

        val result = engine.organize(analysis.screenshot, analysis)

        assertNotNull(result.subcategory)
        assertEquals("Goa Trip", result.subcategory)
    }
}

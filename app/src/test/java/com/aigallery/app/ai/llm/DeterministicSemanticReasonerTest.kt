package com.aigallery.app.ai.llm

import com.aigallery.app.data.database.ScreenshotEntityItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeterministicSemanticReasonerTest {

    @Test
    fun testTravelBookingScenario() {
        val context = LLMContext(
            screenshotId = 1L,
            ocrText = "Booking confirmed at Taj Exotica Goa for 18 Aug. Total ₹12,450 paid.",
            entities = listOf(
                ScreenshotEntityItem(id = 1, screenshotId = 1, type = "LOCATION", value = "Goa"),
                ScreenshotEntityItem(id = 2, screenshotId = 1, type = "ORGANIZATION", value = "Taj Exotica"),
                ScreenshotEntityItem(id = 3, screenshotId = 1, type = "MONEY", value = "₹12,450"),
                ScreenshotEntityItem(id = 4, screenshotId = 1, type = "DATE", value = "18 Aug")
            ),
            category = "TRAVEL",
            platform = "MakeMyTrip"
        )

        val understanding = DeterministicSemanticReasoner.reason(context)

        assertEquals(ScreenshotIntent.BOOKING, understanding.intent)
        assertEquals(ImportanceLevel.HIGH, understanding.importance)
        assertTrue(understanding.title?.contains("Taj Exotica") == true || understanding.title?.contains("Goa") == true)
        assertEquals("Goa", understanding.facts["Destination"])
        assertEquals("₹12,450", understanding.facts["Amount"])
    }

    @Test
    fun testShoppingPurchaseScenario() {
        val context = LLMContext(
            screenshotId = 2L,
            ocrText = "Order confirmed! Sony WH-1000XM5 headphones shipped. Paid ₹8,999",
            entities = listOf(
                ScreenshotEntityItem(id = 5, screenshotId = 2, type = "PRODUCT", value = "Sony Headphones"),
                ScreenshotEntityItem(id = 6, screenshotId = 2, type = "MONEY", value = "₹8,999")
            ),
            category = "SHOPPING",
            platform = "Amazon"
        )

        val understanding = DeterministicSemanticReasoner.reason(context)

        assertEquals(ScreenshotIntent.PURCHASE, understanding.intent)
        assertTrue(understanding.title?.contains("Amazon") == true || understanding.title?.contains("Headphones") == true)
        assertEquals("₹8,999", understanding.facts["Total Paid"])
    }

    @Test
    fun testWorkInternshipScenario() {
        val context = LLMContext(
            screenshotId = 3L,
            ocrText = "Congratulations! Software Engineering Internship offer at Google",
            entities = listOf(
                ScreenshotEntityItem(id = 7, screenshotId = 3, type = "ORGANIZATION", value = "Google"),
                ScreenshotEntityItem(id = 8, screenshotId = 3, type = "TOPIC", value = "Internship")
            ),
            category = "WORK",
            platform = "Gmail"
        )

        val understanding = DeterministicSemanticReasoner.reason(context)

        assertEquals(ScreenshotIntent.WORK, understanding.intent)
        assertEquals(ImportanceLevel.HIGH, understanding.importance)
        assertTrue(understanding.title?.contains("Internship") == true)
        assertEquals("Google", understanding.facts["Company / Tool"])
    }

    @Test
    fun testFinancePaymentScenario() {
        val context = LLMContext(
            screenshotId = 4L,
            ocrText = "Paid ₹3,500 successfully via UPI to Electricity Board",
            entities = listOf(
                ScreenshotEntityItem(id = 9, screenshotId = 4, type = "MONEY", value = "₹3,500"),
                ScreenshotEntityItem(id = 10, screenshotId = 4, type = "ORGANIZATION", value = "Electricity Board")
            ),
            category = "FINANCE",
            platform = "Google Pay"
        )

        val understanding = DeterministicSemanticReasoner.reason(context)

        assertEquals(ScreenshotIntent.FINANCE, understanding.intent)
        assertEquals(ImportanceLevel.HIGH, understanding.importance)
        assertEquals("₹3,500", understanding.facts["Amount"])
    }
}

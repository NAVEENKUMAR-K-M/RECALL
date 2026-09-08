package com.aigallery.app.ai.llm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LLMOutputParserTest {

    @Test
    fun testValidJsonParsing() {
        val json = """
            {
              "title": "Goa Hotel Booking",
              "summary": "Hotel reservation for Taj Exotica in Goa.",
              "topic": "Travel",
              "intent": "BOOKING",
              "importance": "HIGH",
              "keywords": ["Goa", "Hotel", "Taj Exotica"],
              "facts": {
                "Location": "Goa",
                "Hotel": "Taj Exotica",
                "Amount": "₹12,450"
              }
            }
        """.trimIndent()

        val result = LLMOutputParser.parse(json)
        assertTrue(result.isSuccess)
        val understanding = result.getOrThrow()

        assertEquals("Goa Hotel Booking", understanding.title)
        assertEquals("Hotel reservation for Taj Exotica in Goa.", understanding.summary)
        assertEquals("Travel", understanding.topic)
        assertEquals(ScreenshotIntent.BOOKING, understanding.intent)
        assertEquals(ImportanceLevel.HIGH, understanding.importance)
        assertEquals(3, understanding.keywords.size)
        assertEquals("Taj Exotica", understanding.facts["Hotel"])
    }

    @Test
    fun testMarkdownWrappedJsonParsing() {
        val raw = """
            Here is the requested output:
            ```json
            {
              "title": "Sony Headphones Purchase",
              "summary": "Amazon order confirmation for Sony WH-1000XM5.",
              "topic": "Shopping",
              "intent": "PURCHASE",
              "importance": "MEDIUM",
              "keywords": ["Sony", "Headphones", "Amazon"],
              "facts": {
                "Store": "Amazon",
                "Price": "₹8,999"
              }
            }
            ```
        """.trimIndent()

        val result = LLMOutputParser.parse(raw)
        assertTrue(result.isSuccess)
        val understanding = result.getOrThrow()

        assertEquals("Sony Headphones Purchase", understanding.title)
        assertEquals(ScreenshotIntent.PURCHASE, understanding.intent)
        assertEquals(ImportanceLevel.MEDIUM, understanding.importance)
    }

    @Test
    fun testMissingFieldsFallback() {
        val json = """
            {
              "title": "Internship Email"
            }
        """.trimIndent()

        val result = LLMOutputParser.parse(json)
        assertTrue(result.isSuccess)
        val understanding = result.getOrThrow()

        assertEquals("Internship Email", understanding.title)
        assertEquals(ScreenshotIntent.OTHER, understanding.intent)
        assertEquals(ImportanceLevel.MEDIUM, understanding.importance)
        assertTrue(understanding.keywords.isEmpty())
    }

    @Test
    fun testMalformedJsonReturnsFailure() {
        val invalid = "This is not json at all"
        val result = LLMOutputParser.parse(invalid)
        assertTrue(result.isFailure)
    }

    @Test
    fun testStringLengthClamping() {
        val oversizedTitle = "A".repeat(150)
        val json = """
            {
              "title": "$oversizedTitle",
              "summary": "Valid summary"
            }
        """.trimIndent()

        val result = LLMOutputParser.parse(json)
        assertTrue(result.isSuccess)
        val understanding = result.getOrThrow()
        assertEquals(100, understanding.title?.length)
    }
}

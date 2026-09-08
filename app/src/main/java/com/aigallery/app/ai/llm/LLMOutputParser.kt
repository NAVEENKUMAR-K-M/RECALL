package com.aigallery.app.ai.llm

import org.json.JSONArray
import org.json.JSONObject

object LLMOutputParser {

    fun parse(rawOutput: String): Result<LLMUnderstanding> {
        val sanitized = extractJsonPayload(rawOutput)
        if (sanitized.isBlank()) {
            return Result.failure(IllegalArgumentException("Empty or invalid LLM response"))
        }

        return try {
            val json = JSONObject(sanitized)

            val rawTitle = json.optString("title", "").trim()
            val title = if (rawTitle.isNotEmpty() && rawTitle != "null") {
                rawTitle.take(100)
            } else null

            val rawSummary = json.optString("summary", "").trim()
            val summary = if (rawSummary.isNotEmpty() && rawSummary != "null") {
                rawSummary.take(250)
            } else null

            val rawTopic = json.optString("topic", "").trim()
            val topic = if (rawTopic.isNotEmpty() && rawTopic != "null") {
                rawTopic.take(80)
            } else null

            val rawIntent = json.optString("intent", "OTHER")
            val intent = ScreenshotIntent.fromString(rawIntent)

            val rawImportance = json.optString("importance", "MEDIUM")
            val importance = ImportanceLevel.fromString(rawImportance)

            val keywords = mutableListOf<String>()
            val keywordsArray = json.optJSONArray("keywords")
            if (keywordsArray != null) {
                for (i in 0 until minOf(keywordsArray.length(), 10)) {
                    val kw = keywordsArray.optString(i, "").trim()
                    if (kw.isNotEmpty() && kw != "null") {
                        keywords.add(kw.take(50))
                    }
                }
            }

            val facts = mutableMapOf<String, String>()
            val factsObject = json.optJSONObject("facts")
            if (factsObject != null) {
                val keys = factsObject.keys()
                var count = 0
                while (keys.hasNext() && count < 8) {
                    val key = keys.next()
                    val value = factsObject.optString(key, "").trim()
                    if (value.isNotEmpty() && value != "null") {
                        facts[key.take(40)] = value.take(100)
                        count++
                    }
                }
            }

            Result.success(
                LLMUnderstanding(
                    title = title,
                    summary = summary,
                    topic = topic,
                    intent = intent,
                    importance = importance,
                    keywords = keywords,
                    facts = facts
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractJsonPayload(raw: String): String {
        var text = raw.trim()

        // Handle ```json ... ``` or ``` ... ```
        if (text.contains("```json")) {
            val startIndex = text.indexOf("```json") + 7
            val endIndex = text.indexOf("```", startIndex)
            if (endIndex > startIndex) {
                text = text.substring(startIndex, endIndex).trim()
            }
        } else if (text.contains("```")) {
            val startIndex = text.indexOf("```") + 3
            val endIndex = text.indexOf("```", startIndex)
            if (endIndex > startIndex) {
                text = text.substring(startIndex, endIndex).trim()
            }
        }

        // Find outer curly braces
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1)
        }

        return text
    }
}

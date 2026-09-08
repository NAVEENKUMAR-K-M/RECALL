package com.aigallery.app.ai.llm

interface LLMPromptBuilder {
    fun buildScreenshotUnderstandingPrompt(context: LLMContext): String
    fun buildSystemPrompt(): String
}

class LLMPromptBuilderImpl : LLMPromptBuilder {

    override fun buildSystemPrompt(): String {
        return """
You are an on-device screenshot intelligence model in an offline Android Gallery app.
Analyze the provided OCR text and extracted entities from a screenshot.
Respond strictly in valid JSON format.
Rules:
1. Infer only facts directly verifiable from the provided OCR text and entities. Do NOT hallucinate or assume details not present.
2. Keep the summary under 200 characters.
3. Keep the title under 60 characters.
4. If a field is unknown, set it to null.
5. Do NOT include markdown code fences or conversational text. Output pure JSON only.

Expected JSON schema:
{
  "title": "Concise Descriptive Title",
  "summary": "1-2 sentence summary of what this screenshot captures",
  "topic": "Specific Topic (e.g. Goa Trip, Internship, Soundcore Order)",
  "intent": "BOOKING | PURCHASE | WORK | EDUCATION | SOCIAL | FINANCE | TRAVEL | DOCUMENT | COMMUNICATION | OTHER",
  "importance": "LOW | MEDIUM | HIGH | CRITICAL",
  "keywords": ["keyword1", "keyword2", "keyword3"],
  "facts": {
    "key1": "verifiable fact 1",
    "key2": "verifiable fact 2"
  }
}
""".trimIndent()
    }

    override fun buildScreenshotUnderstandingPrompt(context: LLMContext): String {
        val ocrSnippet = context.ocrText?.trim()?.take(2000) ?: "None"
        val entityList = if (context.entities.isNotEmpty()) {
            context.entities.take(15).joinToString(", ") { "${it.type}: ${it.value}" }
        } else {
            "None"
        }
        val tagList = if (context.tags.isNotEmpty()) context.tags.take(10).joinToString(", ") else "None"

        return """
[INPUT EVIDENCE]
Category: ${context.category ?: "UNKNOWN"}
Platform: ${context.platform ?: "UNKNOWN"}
Detected Entities: $entityList
Tags: $tagList
OCR Text:
$ocrSnippet
[END EVIDENCE]

Generate the JSON understanding now:
""".trimIndent()
    }
}

package com.aigallery.app.ai.llm

enum class ScreenshotIntent(val displayName: String) {
    BOOKING("Booking"),
    PURCHASE("Purchase"),
    WORK("Work"),
    EDUCATION("Education"),
    SOCIAL("Social"),
    FINANCE("Finance"),
    TRAVEL("Travel"),
    DOCUMENT("Document"),
    COMMUNICATION("Communication"),
    OTHER("General");

    companion object {
        fun fromString(raw: String?): ScreenshotIntent {
            if (raw.isNullOrBlank()) return OTHER
            val normalized = raw.trim().uppercase().replace(" ", "_")
            return entries.firstOrNull { it.name == normalized } ?: OTHER
        }
    }
}

enum class ImportanceLevel(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");

    companion object {
        fun fromString(raw: String?): ImportanceLevel {
            if (raw.isNullOrBlank()) return MEDIUM
            val normalized = raw.trim().uppercase()
            return entries.firstOrNull { it.name == normalized } ?: MEDIUM
        }
    }
}

data class LLMUnderstanding(
    val title: String?,
    val summary: String?,
    val topic: String?,
    val intent: ScreenshotIntent = ScreenshotIntent.OTHER,
    val importance: ImportanceLevel = ImportanceLevel.MEDIUM,
    val keywords: List<String> = emptyList(),
    val facts: Map<String, String> = emptyMap()
)

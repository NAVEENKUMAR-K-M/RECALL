package com.aigallery.app.ai.organization

import com.aigallery.app.data.database.ScreenshotEntity
import com.aigallery.app.data.database.ScreenshotWithAI
import com.aigallery.app.domain.organization.model.OrganizationResult
import com.aigallery.app.domain.organization.model.ScreenshotCategory

interface ScreenshotOrganizationEngine {

    suspend fun organize(
        screenshot: ScreenshotEntity,
        analysis: ScreenshotWithAI,
        categoryCounts: Map<String, Int> = emptyMap(),
        minScreenshotsForSubfolder: Int = 3,
        organizeLowConfidence: Boolean = false
    ): OrganizationResult
}

class ScreenshotOrganizationEngineImpl : ScreenshotOrganizationEngine {

    override suspend fun organize(
        screenshot: ScreenshotEntity,
        analysis: ScreenshotWithAI,
        categoryCounts: Map<String, Int>,
        minScreenshotsForSubfolder: Int,
        organizeLowConfidence: Boolean
    ): OrganizationResult {
        val classification = analysis.classification
        val rawCategory = classification?.primaryCategory?.uppercase() ?: "OTHER"
        val confidence = classification?.confidence ?: 0.5f

        // 1. Confidence Evaluation
        // Low confidence (< 0.45) defaults to OTHER unless user opted in
        val effectiveCategory = if (confidence < 0.45f && !organizeLowConfidence && rawCategory != "OTHER") {
            ScreenshotCategory.OTHER
        } else {
            mapToStableCategory(rawCategory, analysis)
        }

        // 2. Secondary Categories
        val secondaryCategories = mutableListOf<ScreenshotCategory>()
        classification?.secondaryCategories?.split(",")?.map { it.trim().uppercase() }?.forEach { sec ->
            val mapped = mapToStableCategory(sec, analysis)
            if (mapped != effectiveCategory && mapped != ScreenshotCategory.OTHER && !secondaryCategories.contains(mapped)) {
                secondaryCategories.add(mapped)
            }
        }

        // Detect secondary tickets/travel synergy
        val hasTicketEntity = analysis.tags.any { it.tag.equals("Tickets", ignoreCase = true) } ||
                analysis.entities.any { it.value.contains("Ticket", ignoreCase = true) || it.value.contains("Booking", ignoreCase = true) }
        if (effectiveCategory == ScreenshotCategory.TRAVEL && hasTicketEntity && !secondaryCategories.contains(ScreenshotCategory.TICKETS)) {
            secondaryCategories.add(ScreenshotCategory.TICKETS)
        }

        // 3. Intelligent Subcategory Extraction
        val subcategory = extractSmartSubcategory(
            category = effectiveCategory,
            analysis = analysis,
            categoryCount = categoryCounts[effectiveCategory.id] ?: 0,
            threshold = minScreenshotsForSubfolder
        )

        // 4. Traceable Reason
        val reason = buildReason(effectiveCategory, subcategory, analysis)

        return OrganizationResult(
            primaryCategory = effectiveCategory,
            subcategory = subcategory,
            confidence = confidence,
            secondaryCategories = secondaryCategories,
            reason = reason
        )
    }

    private fun mapToStableCategory(
        raw: String,
        analysis: ScreenshotWithAI
    ): ScreenshotCategory {
        val direct = when (raw) {
            "WORK" -> ScreenshotCategory.WORK
            "EDUCATION" -> ScreenshotCategory.EDUCATION
            "SHOPPING" -> ScreenshotCategory.SHOPPING
            "TRAVEL" -> ScreenshotCategory.TRAVEL
            "FINANCE" -> ScreenshotCategory.FINANCE
            "SOCIAL_MEDIA" -> ScreenshotCategory.SOCIAL_MEDIA
            "ENTERTAINMENT" -> ScreenshotCategory.ENTERTAINMENT
            "TECHNOLOGY" -> ScreenshotCategory.TECHNOLOGY
            "MESSAGING" -> ScreenshotCategory.MESSAGING
            "DOCUMENT", "DOCUMENTS" -> ScreenshotCategory.DOCUMENTS
            "FOOD" -> ScreenshotCategory.FOOD
            "TICKETS", "TICKET" -> ScreenshotCategory.TICKETS
            "IMPORTANT" -> ScreenshotCategory.IMPORTANT
            else -> null
        }
        if (direct != null) return direct

        // Platform heuristic
        val platformCat = when (analysis.platform?.platform?.lowercase()) {
            "instagram", "linkedin", "twitter", "x" -> ScreenshotCategory.SOCIAL_MEDIA
            "whatsapp", "messages", "telegram" -> ScreenshotCategory.MESSAGING
            "amazon", "flipkart", "myntra", "zepto", "blinkit" -> ScreenshotCategory.SHOPPING
            "zomato", "swiggy" -> ScreenshotCategory.FOOD
            "gmail", "outlook", "slack", "teams" -> ScreenshotCategory.WORK
            "paytm", "gpay", "phonepe", "cred" -> ScreenshotCategory.FINANCE
            "youtube", "netflix", "spotify" -> ScreenshotCategory.ENTERTAINMENT
            else -> null
        }
        if (platformCat != null) return platformCat

        // LLM Topic & Intent & Tags heuristic
        val topic = analysis.llmUnderstanding?.topic?.lowercase() ?: ""
        val intent = analysis.llmUnderstanding?.intent?.lowercase() ?: ""
        val allTags = analysis.tags.map { it.tag.lowercase() }
        val combinedText = "$topic $intent ${allTags.joinToString(" ")}"

        return when {
            combinedText.contains("food") || combinedText.contains("recipe") || combinedText.contains("restaurant") || combinedText.contains("swiggy") || combinedText.contains("zomato") -> ScreenshotCategory.FOOD
            combinedText.contains("finance") || combinedText.contains("payment") || combinedText.contains("upi") || combinedText.contains("receipt") || combinedText.contains("bank") || combinedText.contains("invoice") -> ScreenshotCategory.FINANCE
            combinedText.contains("shop") || combinedText.contains("product") || combinedText.contains("cart") || combinedText.contains("order") || combinedText.contains("discount") -> ScreenshotCategory.SHOPPING
            combinedText.contains("message") || combinedText.contains("chat") || combinedText.contains("conversation") -> ScreenshotCategory.MESSAGING
            combinedText.contains("work") || combinedText.contains("career") || combinedText.contains("job") || combinedText.contains("internship") || combinedText.contains("resume") -> ScreenshotCategory.WORK
            combinedText.contains("education") || combinedText.contains("course") || combinedText.contains("lecture") || combinedText.contains("study") || combinedText.contains("notes") -> ScreenshotCategory.EDUCATION
            combinedText.contains("ticket") || combinedText.contains("flight") || combinedText.contains("boarding") || combinedText.contains("hotel") || combinedText.contains("train") -> ScreenshotCategory.TICKETS
            combinedText.contains("travel") || combinedText.contains("trip") || combinedText.contains("destination") -> ScreenshotCategory.TRAVEL
            combinedText.contains("doc") || combinedText.contains("certificate") || combinedText.contains("passport") || combinedText.contains("aadhaar") || combinedText.contains("pan") -> ScreenshotCategory.DOCUMENTS
            combinedText.contains("tech") || combinedText.contains("code") || combinedText.contains("developer") || combinedText.contains("github") -> ScreenshotCategory.TECHNOLOGY
            combinedText.contains("social") || combinedText.contains("post") || combinedText.contains("tweet") || combinedText.contains("reel") -> ScreenshotCategory.SOCIAL_MEDIA
            combinedText.contains("movie") || combinedText.contains("game") || combinedText.contains("music") || combinedText.contains("video") -> ScreenshotCategory.ENTERTAINMENT
            else -> ScreenshotCategory.OTHER
        }
    }

    private fun extractSmartSubcategory(
        category: ScreenshotCategory,
        analysis: ScreenshotWithAI,
        categoryCount: Int,
        threshold: Int
    ): String? {
        if (category == ScreenshotCategory.OTHER) return null

        // Candidates from high-value entities
        val candidates = mutableListOf<String>()

        // Specific category entity extractions
        when (category) {
            ScreenshotCategory.TRAVEL -> {
                // Look for LOCATION entities or hotel/flight keywords
                analysis.entities.filter { it.type == "LOCATION" }.forEach { candidates.add(it.value) }
            }
            ScreenshotCategory.SHOPPING -> {
                // Look for merchant platforms / organizations (Amazon, Flipkart, Elitehubs)
                analysis.entities.filter { it.type == "ORGANIZATION" }.forEach { candidates.add(it.value) }
                analysis.platform?.platform?.let { candidates.add(it) }
            }
            ScreenshotCategory.WORK -> {
                // Look for TOPIC or ORGANIZATION (e.g. Google, Internship, Hackathon)
                analysis.entities.filter { it.type == "TOPIC" || it.type == "ORGANIZATION" }
                    .forEach { candidates.add(it.value) }
            }
            ScreenshotCategory.SOCIAL_MEDIA -> {
                analysis.platform?.platform?.let { candidates.add(it) }
            }
            ScreenshotCategory.TECHNOLOGY -> {
                analysis.entities.filter { it.type == "ORGANIZATION" || it.type == "TOPIC" }
                    .forEach { candidates.add(it.value) }
            }
            ScreenshotCategory.FOOD -> {
                analysis.entities.filter { it.type == "ORGANIZATION" }.forEach { candidates.add(it.value) }
            }
            else -> {
                analysis.entities.filter { it.type == "ORGANIZATION" || it.type == "TOPIC" }
                    .forEach { candidates.add(it.value) }
            }
        }

        // Clean and normalize candidate
        val bestCandidate = candidates
            .map { cleanSubcategoryName(it) }
            .firstOrNull { it.isNotBlank() && it.length in 3..25 && !it.contains("\n") }

        return bestCandidate
    }

    private fun cleanSubcategoryName(raw: String): String {
        return raw.trim()
            .replace(Regex("[^a-zA-Z0-9 _-]"), "")
            .split(" ")
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase() } }
    }

    private fun buildReason(
        category: ScreenshotCategory,
        subcategory: String?,
        analysis: ScreenshotWithAI
    ): String {
        val platformStr = analysis.platform?.platform?.let { "on $it" } ?: ""
        val topEntities = analysis.entities.take(3).joinToString(", ") { "${it.type}:${it.value}" }
        return if (subcategory != null) {
            "Identified as ${category.displayName} ($subcategory) $platformStr [Entities: $topEntities]"
        } else {
            "Identified as ${category.displayName} $platformStr [Entities: $topEntities]"
        }.trim()
    }
}

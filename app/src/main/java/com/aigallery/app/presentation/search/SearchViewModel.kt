package com.aigallery.app.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aigallery.app.ai.embedding.LocalSemanticEmbeddingModel
import com.aigallery.app.data.database.ScreenshotWithAI
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.model.MediaType
import com.aigallery.app.domain.repository.MediaRepository
import com.aigallery.app.domain.repository.ScreenshotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

enum class SearchFilter(val displayName: String) {
    ALL("All"),
    SCREENSHOTS("Screenshots"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    CAMERA("Camera"),
    DOWNLOADS("Downloads")
}

data class SearchResultItem(
    val mediaItem: MediaItem,
    val aiMatchReason: String? = null,
    val matchScore: Float = 0f
)

data class SearchUiState(
    val query: String = "",
    val activeFilter: SearchFilter = SearchFilter.ALL,
    val results: List<SearchResultItem> = emptyList(),
    val isSearching: Boolean = false,
    val recentSearches: List<String> = listOf("Goa booking", "Sony headphones", "Google internship", "GitHub", "Tickets", "Invoice")
)

class SearchViewModel(
    private val mediaRepository: MediaRepository,
    private val screenshotRepository: ScreenshotRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _activeFilter = MutableStateFlow(SearchFilter.ALL)
    val activeFilter: StateFlow<SearchFilter> = _activeFilter.asStateFlow()

    private val embeddingModel = LocalSemanticEmbeddingModel()

    data class SearchQueryState(val rawQuery: String, val filter: SearchFilter)

    // Reactive flow combining MediaStore items with Room Screenshot Intelligence
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<SearchUiState> = combine(_query, _activeFilter) { q, filter ->
        SearchQueryState(q, filter)
    }.debounce(150L)
    .flatMapLatest { (rawQuery, filter) ->
        val trimmed = rawQuery.trim()
        val intent = extractSearchIntent(trimmed)
        val matchedCategory = findMatchingCategory(intent)

        val aiSearchFlow = if (intent.isNotEmpty()) {
            if (matchedCategory != null) {
                combine(
                    screenshotRepository.searchScreenshots(intent),
                    screenshotRepository.getScreenshotsByCategory(matchedCategory)
                ) { byQuery, byCat ->
                    (byQuery + byCat).distinctBy { it.screenshot.screenshotId }
                }
            } else {
                screenshotRepository.searchScreenshots(intent)
            }
        } else {
            flowOf(emptyList())
        }

        combine(mediaRepository.getAllMedia(), aiSearchFlow) { allMedia, aiScreenshots ->
            val mediaMap = allMedia.associateBy { it.id }

            val resultItems = mutableListOf<SearchResultItem>()
            val processedIds = mutableSetOf<Long>()

            // 1. Perform Hybrid Ranking across AI Screenshot Intelligence
            if (trimmed.isNotEmpty()) {
                val queryEmbedding = embeddingModel.generateEmbedding(
                    text = trimmed,
                    tokens = intent.split(" ")
                )

                val scoredScreenshots = aiScreenshots.mapNotNull { ai ->
                    val mediaItem = mediaMap[ai.screenshot.mediaId] ?: return@mapNotNull null
                    
                    // Semantic Similarity Score
                    val semanticScore = if (ai.embedding != null) {
                        val vector = embeddingModel.deserialize(ai.embedding.embedding)
                        embeddingModel.cosineSimilarity(queryEmbedding, vector).coerceAtLeast(0f)
                    } else 0f

                    // Entity Match Score
                    val entityScore = if (ai.entities.any { it.value.contains(intent, ignoreCase = true) || intent.contains(it.value, ignoreCase = true) }) 1.0f else 0f

                    // Keyword / Title / Summary Score
                    val keywordScore = when {
                        ai.llmUnderstanding?.title?.contains(intent, ignoreCase = true) == true -> 1.0f
                        ai.llmUnderstanding?.topic?.contains(intent, ignoreCase = true) == true -> 0.9f
                        ai.llmUnderstanding?.summary?.contains(intent, ignoreCase = true) == true -> 0.7f
                        ai.tags.any { it.tag.contains(intent, ignoreCase = true) } -> 0.6f
                        ai.textEntity?.text?.contains(intent, ignoreCase = true) == true -> 0.5f
                        else -> 0f
                    }

                    // Category Score
                    val categoryScore = if (matchedCategory != null && (ai.classification?.primaryCategory == matchedCategory || ai.classification?.secondaryCategories?.contains(matchedCategory) == true)) 1.0f else 0f

                    // Composite Hybrid Ranking:
                    // FinalScore = 0.40 * semantic + 0.30 * entity + 0.15 * keyword + 0.15 * category
                    val finalScore = (0.40f * semanticScore) + (0.30f * entityScore) + (0.15f * keywordScore) + (0.15f * categoryScore)

                    Triple(mediaItem, ai, finalScore)
                }.sortedByDescending { it.third }

                for ((mediaItem, ai, score) in scoredScreenshots) {
                    if (processedIds.add(mediaItem.id)) {
                        val reason = determineMatchReason(ai, trimmed, score)
                        resultItems.add(SearchResultItem(mediaItem = mediaItem, aiMatchReason = reason, matchScore = score))
                    }
                }
            }

            // 2. Add standard gallery items matching query and/or filter
            val filteredStandard = allMedia.filter { item ->
                if (processedIds.contains(item.id)) return@filter false

                val matchesFilter = when (filter) {
                    SearchFilter.ALL -> true
                    SearchFilter.SCREENSHOTS -> item.relativePath.contains("Screenshots", ignoreCase = true) ||
                            item.filename.contains("screenshot", ignoreCase = true)
                    SearchFilter.PHOTOS -> item.mediaType == MediaType.IMAGE
                    SearchFilter.VIDEOS -> item.mediaType == MediaType.VIDEO
                    SearchFilter.CAMERA -> item.relativePath.contains("DCIM", ignoreCase = true) ||
                            item.albumName.contains("Camera", ignoreCase = true)
                    SearchFilter.DOWNLOADS -> item.relativePath.contains("Download", ignoreCase = true) ||
                            item.albumName.contains("Download", ignoreCase = true)
                    else -> true
                }

                if (!matchesFilter) return@filter false

                if (trimmed.isEmpty()) {
                    true
                } else {
                    item.filename.contains(trimmed, ignoreCase = true) ||
                            item.albumName.contains(trimmed, ignoreCase = true) ||
                            item.relativePath.contains(trimmed, ignoreCase = true)
                }
            }

            for (item in filteredStandard) {
                resultItems.add(SearchResultItem(mediaItem = item))
            }

            SearchUiState(
                query = rawQuery,
                activeFilter = filter,
                results = resultItems,
                isSearching = rawQuery.isNotEmpty() || filter != SearchFilter.ALL
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    private fun determineMatchReason(ai: ScreenshotWithAI, query: String, score: Float): String {
        val q = query.lowercase()

        // 1. LLM Title & Topic
        ai.llmUnderstanding?.let { llm ->
            if (!llm.title.isNullOrBlank() && llm.title.lowercase().contains(q)) {
                return llm.title
            }
            if (!llm.topic.isNullOrBlank() && llm.topic.lowercase().contains(q)) {
                return llm.topic
            }
        }

        // 2. High Semantic Embedding Match
        if (score >= 0.55f && ai.llmUnderstanding?.title != null) {
            return ai.llmUnderstanding.title
        }

        // 3. Entity match
        ai.entities.firstOrNull { it.value.lowercase().contains(q) }?.let {
            return it.value
        }

        // 4. Category match
        ai.classification?.let { cls ->
            if (cls.primaryCategory.lowercase().contains(q)) {
                return cls.primaryCategory
            }
            val sec = cls.secondaryCategories.split(",").firstOrNull { it.lowercase().contains(q) }
            if (sec != null) return sec
        }

        // 5. Platform match
        ai.platform?.let { plt ->
            if (plt.platform.lowercase().contains(q)) {
                return plt.platform
            }
        }

        // 6. Tag match
        ai.tags.firstOrNull { it.tag.lowercase().contains(q) }?.let {
            return "#${it.tag}"
        }

        // 7. OCR text
        if (ai.textEntity?.text?.contains(q, ignoreCase = true) == true) {
            return "Text match"
        }

        return "Matched"
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onFilterSelect(filter: SearchFilter) {
        _activeFilter.value = filter
    }

    fun clearQuery() {
        _query.value = ""
    }

    private fun extractSearchIntent(rawQuery: String): String {
        var q = rawQuery.trim().lowercase()
        val removePrefixes = listOf(
            "show me my ", "show my ", "show me ", "show all ", "show ",
            "find my ", "find all ", "find ", "where is my ", "where is that ", "where is ",
            "where are my ", "get my ", "get ", "display ", "search for ", "search "
        )
        for (prefix in removePrefixes) {
            if (q.startsWith(prefix)) {
                q = q.removePrefix(prefix).trim()
                break
            }
        }
        val removeSuffixes = listOf(
            " screenshots", " screenshot", " photos", " images", " pics", " details"
        )
        for (suffix in removeSuffixes) {
            if (q.endsWith(suffix)) {
                q = q.removeSuffix(suffix).trim()
                break
            }
        }
        return q
    }

    private fun findMatchingCategory(intent: String): String? {
        val q = intent.uppercase().replace(" ", "_")
        val knownCategories = listOf(
            "WORK", "EDUCATION", "SHOPPING", "TRAVEL", "FINANCE", "SOCIAL_MEDIA",
            "ENTERTAINMENT", "TECHNOLOGY", "MESSAGING", "DOCUMENTS", "FOOD",
            "TICKETS", "IMPORTANT", "OTHER"
        )
        for (cat in knownCategories) {
            if (cat == q || cat.replace("_", " ") == intent.uppercase()) {
                return cat
            }
        }
        // Aliases
        if (intent.contains("payment") || intent.contains("upi") || intent.contains("bank") || intent.contains("₹") || intent.contains("rs")) return "FINANCE"
        if (intent.contains("flight") || intent.contains("train") || intent.contains("bus") || intent.contains("hotel") || intent.contains("booking") || intent.contains("trip")) return "TRAVEL"
        if (intent.contains("order") || intent.contains("cart") || intent.contains("buy") || intent.contains("purchase")) return "SHOPPING"
        if (intent.contains("code") || intent.contains("tech") || intent.contains("gpu") || intent.contains("github")) return "WORK"
        if (intent.contains("internship") || intent.contains("resume") || intent.contains("offer")) return "WORK"
        if (intent.contains("chat") || intent.contains("message")) return "MESSAGING"
        if (intent.contains("social") || intent.contains("post")) return "SOCIAL_MEDIA"
        return null
    }

    class Factory(
        private val mediaRepository: MediaRepository,
        private val screenshotRepository: ScreenshotRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(mediaRepository, screenshotRepository) as T
        }
    }
}


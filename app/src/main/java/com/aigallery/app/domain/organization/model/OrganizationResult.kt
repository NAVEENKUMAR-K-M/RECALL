package com.aigallery.app.domain.organization.model

data class OrganizationResult(
    val primaryCategory: ScreenshotCategory,
    val subcategory: String?,
    val confidence: Float,
    val secondaryCategories: List<ScreenshotCategory> = emptyList(),
    val reason: String? = null
)

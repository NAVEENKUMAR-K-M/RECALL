package com.aigallery.app.ai

import com.aigallery.app.domain.model.MediaItem
import java.util.Locale

data class ScreenshotDetectionResult(
    val isScreenshot: Boolean,
    val confidence: Float,
    val reasons: List<String>
)

object ScreenshotDetector {

    private const val THRESHOLD = 0.60f

    private val SCREENSHOT_FOLDERS = listOf(
        "screenshots",
        "screenshot",
        "screen_capture",
        "screencap",
        "screencaps",
        "captures"
    )

    private val SCREENSHOT_PREFIXES = listOf(
        "screenshot_",
        "screenshot-",
        "screenshot ",
        "screen_",
        "screen-",
        "screencap_",
        "capture_",
        "scr_"
    )

    private val EXCLUDED_PATH_KEYWORDS = listOf(
        "whatsapp",
        "telegram",
        "instagram",
        "download",
        "downloads",
        "facebook",
        "twitter",
        "reddit",
        "snapchat"
    )

    fun evaluate(mediaItem: MediaItem): ScreenshotDetectionResult {
        // Videos are strictly excluded from Screenshot AI pipeline
        if (mediaItem.isVideo) {
            return ScreenshotDetectionResult(
                isScreenshot = false,
                confidence = 0f,
                reasons = listOf("Item is a video")
            )
        }

        val relativePath = mediaItem.relativePath.lowercase(Locale.ROOT)
        val folderName = mediaItem.albumName.lowercase(Locale.ROOT)
        val filename = mediaItem.filename.lowercase(Locale.ROOT)
        val mimeType = mediaItem.mimeType.lowercase(Locale.ROOT)

        // STRICT EXCLUSION: iQOO Gallery organized representations must NEVER be re-detected as screenshots!
        // This prevents any recursive feedback or duplicate copy loops.
        if (relativePath.contains("iqoo gallery") || relativePath.contains("iqoo_gallery") ||
            folderName.contains("iqoo gallery") || folderName.contains("iqoo_gallery") ||
            relativePath.contains("ai gallery") || relativePath.contains("ai_gallery") ||
            folderName.contains("ai gallery") || folderName.contains("ai_gallery")
        ) {
            return ScreenshotDetectionResult(
                isScreenshot = false,
                confidence = 0.0f,
                reasons = listOf("Belongs to iQOO Gallery organized directory - strictly excluded to prevent duplicate loops")
            )
        }

        // Strict exclusion of chat & download media
        for (excluded in EXCLUDED_PATH_KEYWORDS) {
            if (relativePath.contains(excluded) || folderName.contains(excluded)) {
                // Unless explicitly in a Screenshots folder
                val isInExplicitScreenshotFolder = SCREENSHOT_FOLDERS.any { folderName.contains(it) }
                if (!isInExplicitScreenshotFolder) {
                    return ScreenshotDetectionResult(
                        isScreenshot = false,
                        confidence = 0.0f,
                        reasons = listOf("Belongs to excluded folder/path: $excluded")
                    )
                }
            }
        }

        // Camera photo exclusion: files in DCIM/Camera without screenshot keyword
        if ((relativePath.contains("dcim/camera") || relativePath.contains("dcim/100media")) &&
            !filename.contains("screenshot") && !folderName.contains("screenshot")
        ) {
            return ScreenshotDetectionResult(
                isScreenshot = false,
                confidence = 0.0f,
                reasons = listOf("Standard camera photo in DCIM/Camera")
            )
        }

        var score = 0.0f
        val reasons = mutableListOf<String>()

        // 1. Folder Check
        if (SCREENSHOT_FOLDERS.any { folderName == it }) {
            score += 0.55f
            reasons.add("Folder name is an exact screenshot folder: '$folderName'")
        } else if (SCREENSHOT_FOLDERS.any { folderName.contains(it) }) {
            score += 0.40f
            reasons.add("Folder name contains screenshot keyword: '$folderName'")
        }

        // 2. Relative Path Check
        if (relativePath.contains("pictures/screenshots") ||
            relativePath.contains("dcim/screenshots") ||
            relativePath.contains("/screenshots/")
        ) {
            score += 0.45f
            reasons.add("Relative path contains standard screenshot directory: '$relativePath'")
        }

        // 3. Filename Check
        if (SCREENSHOT_PREFIXES.any { prefix -> filename.startsWith(prefix) }) {
            score += 0.50f
            reasons.add("Filename starts with screenshot prefix: '$filename'")
        } else if (filename.contains("screenshot") || filename.contains("screencap")) {
            score += 0.35f
            reasons.add("Filename contains screenshot keyword")
        }

        // Regex check for Screenshot_YYYYMMDD_HHMMSS or Screenshot_YYYY-MM-DD
        val datePatternRegex = Regex(".*screenshot[_-]?\\d{4}[_-]?\\d{2}[_-]?\\d{2}.*")
        if (datePatternRegex.matches(filename)) {
            score += 0.25f
            reasons.add("Filename matches screenshot timestamp pattern")
        }

        // 4. Dimensions / Aspect Ratio Check (typical smartphone screens: 16:9 to 22:9)
        if (mediaItem.width > 0 && mediaItem.height > 0) {
            val maxDim = maxOf(mediaItem.width, mediaItem.height).toFloat()
            val minDim = minOf(mediaItem.width, mediaItem.height).toFloat()
            val aspectRatio = maxDim / minDim

            // Standard smartphone ratios (16:9 ~ 1.77, 19.5:9 ~ 2.16, 20:9 ~ 2.22, 21:9 ~ 2.33)
            if (aspectRatio in 1.75f..2.45f) {
                score += 0.10f
                reasons.add("Dimensions match mobile screen aspect ratio (${"%.2f".format(aspectRatio)}:1)")
            }
        }

        // 5. MIME type check
        if (mimeType == "image/png") {
            // Android default format for screenshots is PNG
            score += 0.05f
            reasons.add("PNG image format commonly used for screenshots")
        }

        val finalConfidence = score.coerceIn(0.0f, 1.0f)
        val isScreenshot = finalConfidence >= THRESHOLD

        return ScreenshotDetectionResult(
            isScreenshot = isScreenshot,
            confidence = finalConfidence,
            reasons = reasons
        )
    }

    fun isScreenshot(mediaItem: MediaItem): Boolean {
        return evaluate(mediaItem).isScreenshot
    }
}

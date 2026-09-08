package com.aigallery.app.ai

import android.graphics.Bitmap
import com.aigallery.app.ai.classification.PlatformDetector
import com.aigallery.app.ai.classification.ScreenshotClassifier
import com.aigallery.app.ai.classification.TagGenerator
import com.aigallery.app.ai.extraction.EntityExtractor
import com.aigallery.app.ai.ocr.MLKitLocalOCRProcessor
import com.aigallery.app.ai.ocr.OCRProcessor
import com.aigallery.app.data.database.ScreenshotClassificationEntity
import com.aigallery.app.data.database.ScreenshotEntityItem
import com.aigallery.app.data.database.ScreenshotPlatformEntity
import com.aigallery.app.data.database.ScreenshotTagEntity
import com.aigallery.app.data.database.TextBlockEntity

data class ScreenshotAnalysisResult(
    val screenshotId: Long,
    val ocrText: String,
    val textBlocks: List<TextBlockEntity> = emptyList(),
    val language: String? = null,
    val entities: List<ScreenshotEntityItem> = emptyList(),
    val classification: ScreenshotClassificationEntity? = null,
    val platform: ScreenshotPlatformEntity? = null,
    val tags: List<ScreenshotTagEntity> = emptyList(),
    val overallConfidence: Float = 1.0f,
    val hardwareBackend: String = "Local CPU"
)

interface ScreenshotAIEngine {
    suspend fun analyze(
        screenshotId: Long,
        bitmap: Bitmap
    ): ScreenshotAnalysisResult

    fun getActiveProvider(): AIModelProvider
}

class ScreenshotAIEngineImpl(
    private val ocrProcessor: OCRProcessor = MLKitLocalOCRProcessor(),
    private val modelProvider: AIModelProvider = LocalCPUModelProvider()
) : ScreenshotAIEngine {

    override fun getActiveProvider(): AIModelProvider = modelProvider

    override suspend fun analyze(
        screenshotId: Long,
        bitmap: Bitmap
    ): ScreenshotAnalysisResult {
        // Step 1: Run on-device OCR
        val ocrResult = ocrProcessor.extractText(bitmap)
        val fullText = ocrResult.fullText

        val textBlocks = ocrResult.textBlocks.map { block ->
            TextBlockEntity(
                screenshotId = screenshotId,
                text = block.text,
                left = block.left,
                top = block.top,
                right = block.right,
                bottom = block.bottom,
                confidence = block.confidence
            )
        }

        // Step 2: Deterministic Entity Extraction
        val entities = EntityExtractor.extract(
            screenshotId = screenshotId,
            text = fullText
        )

        // Step 3: Platform Detection
        val platform = PlatformDetector.detect(
            screenshotId = screenshotId,
            fullText = fullText,
            entities = entities
        )

        // Step 4: Screenshot Classification
        val classification = ScreenshotClassifier.classify(
            screenshotId = screenshotId,
            fullText = fullText,
            entities = entities,
            platform = platform?.platform
        )

        // Step 5: Tag Generation from actual evidence
        val tags = TagGenerator.generate(
            screenshotId = screenshotId,
            entities = entities,
            classification = classification,
            platform = platform,
            fullText = fullText
        )

        return ScreenshotAnalysisResult(
            screenshotId = screenshotId,
            ocrText = fullText,
            textBlocks = textBlocks,
            language = ocrResult.detectedLanguage,
            entities = entities,
            classification = classification,
            platform = platform,
            tags = tags,
            overallConfidence = ocrResult.confidence,
            hardwareBackend = modelProvider.backendName()
        )
    }
}

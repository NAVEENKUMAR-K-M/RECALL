package com.aigallery.app.ai.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TextBlock(
    val text: String,
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val confidence: Float = 1.0f
)

data class OCRResult(
    val fullText: String,
    val textBlocks: List<TextBlock> = emptyList(),
    val confidence: Float = 1.0f,
    val detectedLanguage: String? = null
)

interface OCRProcessor {
    suspend fun extractText(bitmap: Bitmap): OCRResult
}

/**
 * 100% On-Device OCR Processor using Google ML Kit.
 * Strictly operates offline on the local device runtime.
 * Zero network requests or cloud telemetry.
 */
class MLKitLocalOCRProcessor : OCRProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun extractText(bitmap: Bitmap): OCRResult = suspendCancellableCoroutine { continuation ->
        try {
            val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            } else {
                bitmap
            }
            val image = InputImage.fromBitmap(safeBitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text.trim()
                    val blocks = mutableListOf<TextBlock>()
                    var detectedLanguage: String? = null

                    for (block in visionText.textBlocks) {
                        val rect = block.boundingBox
                        if (detectedLanguage == null && block.recognizedLanguage.isNotEmpty()) {
                            detectedLanguage = block.recognizedLanguage
                        }
                        blocks.add(
                            TextBlock(
                                text = block.text,
                                left = rect?.left ?: 0,
                                top = rect?.top ?: 0,
                                right = rect?.right ?: 0,
                                bottom = rect?.bottom ?: 0,
                                confidence = 1.0f
                            )
                        )
                    }

                    continuation.resume(
                        OCRResult(
                            fullText = fullText,
                            textBlocks = blocks,
                            confidence = if (fullText.isNotEmpty()) 0.95f else 0.0f,
                            detectedLanguage = detectedLanguage
                        )
                    )
                }
                .addOnFailureListener { exception ->
                    // Return empty OCR result rather than crashing the pipeline
                    continuation.resume(
                        OCRResult(
                            fullText = "",
                            textBlocks = emptyList(),
                            confidence = 0.0f,
                            detectedLanguage = null
                        )
                    )
                }
        } catch (e: Exception) {
            continuation.resume(
                OCRResult(
                    fullText = "",
                    textBlocks = emptyList(),
                    confidence = 0.0f,
                    detectedLanguage = null
                )
            )
        }
    }
}

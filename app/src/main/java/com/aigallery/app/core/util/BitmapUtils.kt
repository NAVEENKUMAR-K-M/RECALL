package com.aigallery.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BitmapUtils {

    private const val TAG = "BitmapUtils"

    /**
     * Efficiently decodes a downsampled software bitmap from a Content URI.
     * Guarantees a non-hardware bitmap safe for on-device ML Kit OCR inference.
     * Downsamples to a maximum dimension of ~1280px, optimal for mobile OCR.
     */
    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 1280
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val origWidth = info.size.width
                        val origHeight = info.size.height
                        val maxEdge = maxOf(origWidth, origHeight)
                        if (maxEdge > maxDimension) {
                            val scale = maxDimension.toFloat() / maxEdge.toFloat()
                            val targetWidth = (origWidth * scale).toInt().coerceAtLeast(1)
                            val targetHeight = (origHeight * scale).toInt().coerceAtLeast(1)
                            decoder.setTargetSize(targetWidth, targetHeight)
                        }
                        // MUST be software allocator for ML Kit compatibility
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }

                    // Extra safety check for ML Kit
                    if (bitmap.config == Bitmap.Config.HARDWARE) {
                        return@withContext bitmap.copy(Bitmap.Config.ARGB_8888, false)
                    }
                    return@withContext bitmap
                } catch (e: Exception) {
                    Log.w(TAG, "ImageDecoder failed for $uri, falling back to BitmapFactory", e)
                }
            }

            // Fallback for API < 28 or ImageDecoder failure
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val fd = pfd.fileDescriptor
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFileDescriptor(fd, null, options)

                val origWidth = options.outWidth
                val origHeight = options.outHeight
                if (origWidth <= 0 || origHeight <= 0) return@withContext null

                var sampleSize = 1
                val maxEdge = maxOf(origWidth, origHeight)
                while (maxEdge / sampleSize > maxDimension) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeFileDescriptor(fd, null, decodeOptions)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap from URI: $uri", e)
            null
        }
    }
}

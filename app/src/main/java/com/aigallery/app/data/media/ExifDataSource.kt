package com.aigallery.app.data.media

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.model.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExifDataSource(private val context: Context) {

    suspend fun getMetadata(item: MediaItem): MediaMetadata = withContext(Dispatchers.IO) {
        var cameraModel: String? = null
        var lensModel: String? = null
        var focalLength: String? = null
        var aperture: String? = null
        var iso: String? = null
        var shutterSpeed: String? = null
        var latitude: Double? = null
        var longitude: Double? = null

        try {
            context.contentResolver.openInputStream(item.uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                cameraModel = when {
                    make != null && model != null && !model.startsWith(make) -> "$make $model"
                    model != null -> model
                    else -> make
                }

                lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)

                val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                if (focal > 0) {
                    focalLength = String.format("%.1f mm", focal)
                }

                val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, 0.0)
                if (fNumber > 0) {
                    aperture = String.format("ƒ/%.1f", fNumber)
                }

                val isoVal = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                if (!isoVal.isNullOrBlank()) {
                    iso = "ISO $isoVal"
                }

                val expTime = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, 0.0)
                if (expTime > 0) {
                    shutterSpeed = if (expTime < 1.0) {
                        String.format("1/%d s", (1.0 / expTime).toInt())
                    } else {
                        String.format("%.1f s", expTime)
                    }
                }

                val latLong = FloatArray(2)
                if (exif.getLatLong(latLong)) {
                    latitude = latLong[0].toDouble()
                    longitude = latLong[1].toDouble()
                }
            }
        } catch (_: Exception) {
            // Ignore EXIF parsing errors gracefully
        }

        MediaMetadata(
            width = item.width,
            height = item.height,
            size = item.size,
            mimeType = item.mimeType,
            cameraModel = cameraModel,
            lensModel = lensModel,
            focalLength = focalLength,
            aperture = aperture,
            iso = iso,
            shutterSpeed = shutterSpeed,
            latitude = latitude,
            longitude = longitude,
            locationName = if (latitude != null && longitude != null) {
                String.format("%.4f°, %.4f°", latitude, longitude)
            } else null
        )
    }
}

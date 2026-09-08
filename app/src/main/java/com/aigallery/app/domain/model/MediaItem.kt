package com.aigallery.app.domain.model

import android.net.Uri

enum class MediaType {
    IMAGE,
    VIDEO
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val mediaType: MediaType,
    val filename: String,
    val mimeType: String,
    val dateTaken: Long,
    val dateAdded: Long,
    val width: Int,
    val height: Int,
    val size: Long,
    val relativePath: String,
    val albumName: String,
    val bucketId: Long,
    val duration: Long = 0L,
    val isFavorite: Boolean = false
) {
    val isVideo: Boolean get() = mediaType == MediaType.VIDEO

    val formattedDuration: String
        get() {
            if (duration <= 0) return "00:00"
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var s = size.toDouble()
            var unitIndex = 0
            while (s >= 1024 && unitIndex < units.size - 1) {
                s /= 1024
                unitIndex++
            }
            return String.format("%.1f %s", s, units[unitIndex])
        }
}

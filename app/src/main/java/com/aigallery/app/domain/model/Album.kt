package com.aigallery.app.domain.model

import android.net.Uri

data class Album(
    val id: Long,
    val name: String,
    val coverUri: Uri,
    val itemCount: Int,
    val relativePath: String = ""
)

data class MediaGroup(
    val title: String,
    val items: List<MediaItem>
)

data class MediaMetadata(
    val width: Int = 0,
    val height: Int = 0,
    val size: Long = 0L,
    val mimeType: String = "",
    val cameraModel: String? = null,
    val lensModel: String? = null,
    val focalLength: String? = null,
    val aperture: String? = null,
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null
)

package com.aigallery.app.data.media

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.aigallery.app.domain.model.Album
import com.aigallery.app.domain.model.MediaItem
import com.aigallery.app.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreDataSource(private val context: Context) {

    suspend fun queryAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()
        val contentResolver = context.contentResolver

        // 1. Query Images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.RELATIVE_PATH
            } else {
                MediaStore.Images.Media.DATA
            },
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC, ${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            parseMediaItems(cursor, MediaType.IMAGE, mediaList)
        }

        // 2. Query Videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.RELATIVE_PATH
            } else {
                MediaStore.Video.Media.DATA
            },
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )

        contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_TAKEN} DESC, ${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            parseMediaItems(cursor, MediaType.VIDEO, mediaList)
        }

        // 3. Sort unified list chronologically descending
        mediaList.sortByDescending { it.dateTaken.takeIf { t -> t > 0 } ?: (it.dateAdded * 1000) }
        mediaList
    }

    private fun parseMediaItems(
        cursor: Cursor,
        type: MediaType,
        destination: MutableList<MediaItem>
    ) {
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
        val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
        val dateTakenCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
        val dateAddedCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
        val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
        val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
        val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
        val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
        }
        val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val durationCol = if (type == MediaType.VIDEO) cursor.getColumnIndex(MediaStore.Video.Media.DURATION) else -1

        val baseUri = if (type == MediaType.IMAGE) {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "Untitled" else "Untitled"
            val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: if (type == MediaType.IMAGE) "image/jpeg" else "video/mp4" else ""
            val dateTaken = if (dateTakenCol >= 0) cursor.getLong(dateTakenCol) else 0L
            val dateAdded = if (dateAddedCol >= 0) cursor.getLong(dateAddedCol) else 0L
            val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
            val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
            val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
            val path = if (pathCol >= 0) cursor.getString(pathCol) ?: "" else ""

            // Exclude iQOO Gallery / AI Gallery organized copies from the main media roll
            // to avoid visual duplication and prevent recursive detection
            if (path.contains("iQOO Gallery", ignoreCase = true) || path.contains("iQOO_Gallery", ignoreCase = true) ||
                path.contains("AI Gallery", ignoreCase = true) || path.contains("AI_Gallery", ignoreCase = true)) {
                continue
            }

            val bucketId = if (bucketIdCol >= 0) cursor.getLong(bucketIdCol) else 0L
            val bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "Album" else "Album"
            val duration = if (durationCol >= 0) cursor.getLong(durationCol) else 0L

            val itemUri = ContentUris.withAppendedId(baseUri, id)

            destination.add(
                MediaItem(
                    id = id,
                    uri = itemUri,
                    mediaType = type,
                    filename = name,
                    mimeType = mime,
                    dateTaken = if (dateTaken > 0) dateTaken else dateAdded * 1000,
                    dateAdded = dateAdded,
                    width = width,
                    height = height,
                    size = size,
                    relativePath = path,
                    albumName = bucketName,
                    bucketId = bucketId,
                    duration = duration
                )
            )
        }
    }

    suspend fun queryAlbums(mediaItems: List<MediaItem>): List<Album> = withContext(Dispatchers.Default) {
        val groupedByBucket = mediaItems.groupBy { it.bucketId }
        groupedByBucket.mapNotNull { (bucketId, items) ->
            if (items.isEmpty()) return@mapNotNull null
            val first = items.first()
            Album(
                id = bucketId,
                name = first.albumName.ifBlank { "Gallery" },
                coverUri = first.uri,
                itemCount = items.size,
                relativePath = first.relativePath
            )
        }.sortedByDescending { it.itemCount }
    }
}

package com.aigallery.app.data.storage

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.aigallery.app.domain.organization.model.ScreenshotCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MediaStoreOrganizationManager {

    suspend fun createOrganizedCopy(
        sourceUri: Uri,
        filename: String,
        mimeType: String,
        category: ScreenshotCategory,
        subcategory: String? = null
    ): Result<Uri>

    suspend fun removeOrganizedCopy(destinationUri: Uri): Boolean

    suspend fun checkUriExists(uri: Uri): Boolean

    suspend fun cleanAllOrganizedCopies(): Int
}

class MediaStoreOrganizationManagerImpl(
    private val context: Context
) : MediaStoreOrganizationManager {

    private val contentResolver: ContentResolver = context.contentResolver

    override suspend fun createOrganizedCopy(
        sourceUri: Uri,
        filename: String,
        mimeType: String,
        category: ScreenshotCategory,
        subcategory: String?
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            // Guard: Never organize a file that is ALREADY in the iQOO Gallery or AI Gallery folder
            val uriString = sourceUri.toString()
            if (uriString.contains("iQOO%20Gallery") || uriString.contains("iQOO Gallery") ||
                uriString.contains("AI%20Gallery") || uriString.contains("AI Gallery")) {
                return@withContext Result.success(sourceUri)
            }

            // Build safe scoped storage relative path
            val subPath = if (subcategory.isNullOrBlank()) {
                "Pictures/iQOO Gallery/Screenshots/${category.displayName}/"
            } else {
                "Pictures/iQOO Gallery/Screenshots/${category.displayName}/$subcategory/"
            }

            val sanitizedFilename = if (filename.contains(".")) filename else "$filename.png"
            val effectiveMimeType = if (mimeType.isNotBlank()) mimeType else "image/png"

            // IDEMPOTENCY CHECK: Check if this file already exists in MediaStore at this subPath
            val existing = findExistingCopy(subPath, sanitizedFilename)
            if (existing != null) {
                return@withContext Result.success(existing)
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, sanitizedFilename)
                put(MediaStore.Images.Media.MIME_TYPE, effectiveMimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, subPath)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val destUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext Result.failure(Exception("Failed to insert MediaStore record for $subPath"))

            try {
                contentResolver.openInputStream(sourceUri).use { inStream ->
                    if (inStream == null) {
                        contentResolver.delete(destUri, null, null)
                        return@withContext Result.failure(Exception("Cannot open source stream for $sourceUri"))
                    }
                    contentResolver.openOutputStream(destUri).use { outStream ->
                        if (outStream == null) {
                            contentResolver.delete(destUri, null, null)
                            return@withContext Result.failure(Exception("Cannot open destination stream for $destUri"))
                        }
                        inStream.copyTo(outStream)
                    }
                }

                // Release IS_PENDING on Android 10+ so other apps and system MediaStore index it immediately
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val finishValues = ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    contentResolver.update(destUri, finishValues, null, null)
                }

                Result.success(destUri)
            } catch (ioe: Exception) {
                // Rollback inserted destination row on failure
                contentResolver.delete(destUri, null, null)
                Result.failure(ioe)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findExistingCopy(subPath: String, filename: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val selection = "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?"
            val selectionArgs = arrayOf(filename, subPath)

            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun removeOrganizedCopy(destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val rows = contentResolver.delete(destinationUri, null, null)
            rows > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun checkUriExists(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                null
            )?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun cleanAllOrganizedCopies(): Int = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext 0
        try {
            val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR ${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("Pictures/iQOO Gallery/%", "Pictures/AI Gallery/%")
            val deleted = contentResolver.delete(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                selection,
                selectionArgs
            )
            deleted
        } catch (e: Exception) {
            0
        }
    }
}

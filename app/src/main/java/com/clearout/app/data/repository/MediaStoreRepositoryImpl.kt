package com.clearout.app.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.clearout.app.domain.model.Photo
import com.clearout.app.domain.repository.MediaStoreRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MediaStoreRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaStoreRepository {

    override suspend fun getPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        // Query external content uri
        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        context.contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val sizeBytes = cursor.getLong(sizeCol)
                val dateTaken = cursor.getLong(dateCol)
                val bucketName = cursor.getString(bucketCol) ?: "Camera"

                val uri = ContentUris.withAppendedId(contentUri, id)

                // Only include valid image files with sizes greater than 0
                if (sizeBytes > 0) {
                    photos.add(Photo(id, uri, sizeBytes, dateTaken, bucketName))
                }
            }
        }
        photos
    }

    override fun buildDeleteRequest(uris: List<Uri>): IntentSender? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
        } else {
            // For API 29 (Android 10) we can try to catch RecoverableSecurityException when actually deleting.
            // On pre-R, the contentResolver.delete might throw a security exception. 
            // Returning null signals that the repository doesn't have a direct IntentSender, so deletion will be attempted directly or handled via exception.
            null
        }
    }
}

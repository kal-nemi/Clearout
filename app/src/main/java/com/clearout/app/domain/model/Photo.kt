package com.clearout.app.domain.model

import android.net.Uri

data class Photo(
    val id: Long,
    val uri: Uri,
    val sizeBytes: Long,
    val dateTaken: Long,
    val bucketName: String
) {
    val sizeMb: Float get() = sizeBytes / 1_048_576f
}

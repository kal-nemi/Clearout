package com.clearout.app.domain.repository

import android.content.IntentSender
import android.net.Uri
import com.clearout.app.domain.model.Photo

interface MediaStoreRepository {
    suspend fun getPhotos(): List<Photo>
    fun buildDeleteRequest(uris: List<Uri>): IntentSender?
}

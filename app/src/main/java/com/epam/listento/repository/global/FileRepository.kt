package com.epam.listento.repository.global

import android.net.Uri
import retrofit2.Response

interface FileRepository {
    suspend fun downloadTrack(
        trackName: String,
        audioUrl: String,
        completion: suspend (Response<Uri>) -> Unit
    )
}

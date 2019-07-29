package com.epam.listento.repository.global

import android.net.Uri
import retrofit2.Response

interface FileRepository {
    fun downloadTrack(
        trackName: String,
        audioUrl: String,
        completion: (Response<Uri>) -> Unit
    )
}

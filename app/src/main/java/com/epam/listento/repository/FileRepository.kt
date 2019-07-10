package com.epam.listento.repository

import android.net.Uri
import retrofit2.Response

interface FileRepository {
    fun downloadTrack(
        audioUrl: String,
        completion: (Response<Uri>) -> Unit
    )
}

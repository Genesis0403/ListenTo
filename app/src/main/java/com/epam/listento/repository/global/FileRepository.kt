package com.epam.listento.repository.global

import com.epam.listento.api.ApiResponse

interface FileRepository {
    suspend fun downloadTrack(
        trackName: String,
        audioUrl: String
    ): ApiResponse<String>
}

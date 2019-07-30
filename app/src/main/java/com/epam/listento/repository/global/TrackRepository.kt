package com.epam.listento.repository.global

import android.net.Uri
import com.epam.listento.api.ApiResponse
import com.epam.listento.domain.DomainTrack

interface TrackRepository {
    suspend fun fetchTrack(id: Int, isCaching: Boolean, completion: suspend (ApiResponse<DomainTrack>) -> Unit)
    fun checkTrackExistence(trackName: String): Boolean
    fun fetchTrackUri(trackName: String): Uri
}

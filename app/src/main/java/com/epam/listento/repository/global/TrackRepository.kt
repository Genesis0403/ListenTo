package com.epam.listento.repository.global

import com.epam.listento.api.ApiResponse
import com.epam.listento.domain.DomainTrack

interface TrackRepository {
    suspend fun fetchTrack(id: Int, isCaching: Boolean): ApiResponse<DomainTrack>
    fun checkTrackExistence(trackName: String): Boolean
    fun fetchTrackPath(trackName: String): String
}

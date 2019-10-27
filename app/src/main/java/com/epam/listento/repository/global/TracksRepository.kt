package com.epam.listento.repository.global

import androidx.lifecycle.LiveData
import com.epam.listento.api.ApiResponse
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Track

interface TracksRepository {
    suspend fun fetchTracks(text: String): ApiResponse<List<DomainTrack>>
    suspend fun getCache(): LiveData<List<Track>>
}

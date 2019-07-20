package com.epam.listento.repository

import androidx.lifecycle.LiveData
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Track
import kotlinx.coroutines.Job
import retrofit2.Response

interface TracksRepository {
    fun fetchTracks(text: String, completion: (Response<List<DomainTrack>>) -> Unit): Job
    fun cacheTrack(track: Track)
    fun uncacheTrack(track: Track)
    fun getCache(completion: (LiveData<List<Track>>) -> Unit)
}

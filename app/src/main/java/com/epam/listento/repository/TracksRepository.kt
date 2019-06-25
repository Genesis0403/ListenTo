package com.epam.listento.repository

import com.epam.listento.api.model.ApiTrack
import kotlinx.coroutines.Job
import retrofit2.Response

interface TracksRepository {
    fun fetchTracks(text: String, completion: (Response<List<ApiTrack>>) -> Unit): Job
}

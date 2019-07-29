package com.epam.listento.repository.global

import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.LiveData
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Track
import kotlinx.coroutines.Job
import retrofit2.Response

interface TracksRepository {
    fun fetchTracks(text: String, completion: (Response<List<DomainTrack>>) -> Unit): Job
    fun getCache(completion: (LiveData<List<Track>>) -> Unit)
}

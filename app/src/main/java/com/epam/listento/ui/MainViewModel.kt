package com.epam.listento.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.mapTrack
import com.epam.listento.db.TracksDao
import com.epam.listento.model.Track
import com.epam.listento.repository.TracksRepository
import com.epam.listento.utils.ContextProvider
import kotlinx.coroutines.Job
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val contextProvider: ContextProvider,
    private val tracksRepo: TracksRepository,
    tracksDao: TracksDao
) : ViewModel() {

    var lastQuery: String? = null
    private var job: Job? = null

    private val _tracks: MutableLiveData<ApiResponse<List<Track>>> = MutableLiveData()
    val tracks: LiveData<ApiResponse<List<Track>>> get() = _tracks

    val cachedTracks: LiveData<List<Track>> = tracksDao.getTracks()

    fun fetchTracks(text: String) {
        job?.cancel()
        job = tracksRepo.fetchTracks(text) { response ->
            if (response.isSuccessful) {
                val items =
                    response.body()?.asSequence()?.map { mapTrack(it) }?.filterNotNull()?.toList()
                _tracks.postValue(ApiResponse.success(items))
            } else {
                _tracks.postValue(ApiResponse.error(response.message()))
            }
        }
    }

    fun cacheTrack(track: Track) {
        tracksRepo.cacheTrack(track)
    }

    fun uncacheTrack(track: Track) {
        tracksRepo.uncacheTrack(track)
    }
}

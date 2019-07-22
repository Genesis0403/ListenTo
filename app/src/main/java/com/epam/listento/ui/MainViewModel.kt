package com.epam.listento.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.mapTrack
import com.epam.listento.db.TracksDao
import com.epam.listento.model.Track
import com.epam.listento.repository.TracksRepository
import com.epam.listento.utils.ContextProvider
import com.epam.listento.utils.PlatformMappers
import kotlinx.coroutines.*
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val contextProvider: ContextProvider,
    private val tracksRepo: TracksRepository,
    dao: TracksDao,
    mappers: PlatformMappers
) : ViewModel() {

    val lastQuery: MutableLiveData<String> = MutableLiveData()
    private var job: Job? = null
    private var queryJob: Job? = null

    private val _tracks: MutableLiveData<ApiResponse<List<Track>>> = MutableLiveData()
    val tracks: LiveData<ApiResponse<List<Track>>> get() = _tracks

    val cachedTracks: LiveData<List<Track>> = Transformations.switchMap(dao.getTracks()) { domain ->
        val tracks = domain.mapNotNull { mappers.mapTrack(it) }.toList()
        MutableLiveData<List<Track>>().apply {
            value = tracks
        }
    }

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

    fun querySearch(text: String?, onQuery: (String) -> Unit) {
        if (text != null) {
            queryJob?.cancel()
            queryJob = CoroutineScope(Dispatchers.Main).launch {
                delay(500)
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

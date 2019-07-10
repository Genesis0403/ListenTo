package com.epam.listento.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.mapTrack
import com.epam.listento.model.Track
import com.epam.listento.repository.TracksRepository
import com.epam.listento.utils.ContextProvider
import kotlinx.coroutines.Job
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val contextProvider: ContextProvider,
    private val tracksRepository: TracksRepository
) : ViewModel() {

    var lastQuery: String? = null
    private var job: Job? = null

    private val _tracks: MutableLiveData<ApiResponse<List<Track>>> = MutableLiveData()
    val tracks: LiveData<ApiResponse<List<Track>>> get() = _tracks

    fun fetchTracks(text: String) {
        job?.cancel()
        job = tracksRepository.fetchTracks(text) { response ->
            if (response.isSuccessful) {
                val items =
                    response.body()?.asSequence()?.map { mapTrack(it) }?.filterNotNull()?.toList()
                _tracks.postValue(ApiResponse.success(items))
            } else {
                _tracks.postValue(ApiResponse.error(response.message()))
            }
        }
    }
}

package com.epam.listento.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.mapTrack
import com.epam.listento.api.model.ApiStorage
import com.epam.listento.model.Track
import com.epam.listento.repository.*
import com.epam.listento.utils.ContextProvider
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val contextProvider: ContextProvider,
    private val tracksRepository: TracksRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _tracks: MutableLiveData<ApiResponse<List<Track>>> = MutableLiveData()
    val tracks: LiveData<ApiResponse<List<Track>>> get() = _tracks

    private val _url: MutableLiveData<ApiResponse<String>> = MutableLiveData()
    val url: LiveData<ApiResponse<String>> get() = _url

    fun fetchTracks(text: String) {
        val job = tracksRepository.fetchTracks(text) { response ->
            if (response.isSuccessful) {
                val items =
                    response.body()?.asSequence()?.map { mapTrack(it) }?.filterNotNull()?.toList()
                _tracks.postValue(ApiResponse.success(items))
            } else {
                _tracks.postValue(ApiResponse.error(response.message()))
            }
        }
    }

    fun playTrack(track: Track) {
        val job = storageRepository.fetchStorage(track.storageDir) { response ->
            if (response.isSuccessful) {
                response.body()?.let {
                    val downloadUrl = createUrl(it)
                    _url.postValue(ApiResponse.success(downloadUrl))
                }
            }
        }
    }

    private fun createUrl(storage: ApiStorage): String {
// http://{host}/get-mp3/{s}/{ts}{path}
        return StringBuilder("https://").apply {
            append(storage.host)
            append("/get-mp3/")
            append(storage.s)
            append("/")
            append(storage.ts)
            append(storage.path)
        }.toString()
    }
}

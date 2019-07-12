package com.epam.listento.ui

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.mapTrack
import com.epam.listento.model.Track
import com.epam.listento.repository.*
import com.epam.listento.utils.ContextProvider
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val contextProvider: ContextProvider,
    private val tracksRepository: TracksRepository,
    private val storageRepository: StorageRepository,
    private val audioRepository: AudioRepository,
    private val fileRepository: FileRepository
) : ViewModel() {

    var lastQuery: String? = null

    private val _tracks: MutableLiveData<ApiResponse<List<Track>>> = MutableLiveData()
    val tracks: LiveData<ApiResponse<List<Track>>> get() = _tracks

    private val _url: MutableLiveData<ApiResponse<Uri>> = MutableLiveData()
    val url: LiveData<ApiResponse<Uri>> get() = _url

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

    fun downloadTrack(track: Track) {
        val job = storageRepository.fetchStorage(track.storageDir) { response ->
            if (response.isSuccessful) {
                response.body()?.let {
                    val downloadUrl = audioRepository.fetchAudioUrl(it)
                    onDownloadResponse(downloadUrl)
                }
            } else {
                _url.postValue(ApiResponse.error(response.message()))
            }
        }
    }

    private fun onDownloadResponse(downloadUrl: String) {
        fileRepository.downloadTrack(downloadUrl) { response ->
            if (response.isSuccessful) {
                _url.postValue(ApiResponse.success(response.body()))
            } else {
                _url.postValue(ApiResponse.error(response.message()))
            }
        }
    }
}

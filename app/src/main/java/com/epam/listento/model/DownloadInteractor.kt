package com.epam.listento.model

import android.net.Uri
import com.epam.listento.api.ApiResponse
import com.epam.listento.repository.*
import com.epam.listento.utils.ContextProvider
import javax.inject.Inject

class DownloadInteractor @Inject constructor(
    private val contextProvider: ContextProvider,
    private val audioRepo: AudioRepository,
    private val storageRepo: StorageRepository,
    private val fileRepo: FileRepository,
    private val trackRepo: TrackRepository
) {

    private companion object {
        private const val FAILED_TO_LOAD_TRACK = "Failed to load track"
    }

    fun downloadTrack(
        track: Track,
        isCaching: Boolean,
        completion: (ApiResponse<Uri>) -> Unit
    ) {
        val trackName = "${track.artist?.name}-${track.title}.mp3"
        if (trackRepo.checkTrackExistence(trackName)) {
            val uri = trackRepo.fetchTrackUri(trackName)
            completion(ApiResponse.success(uri))
        } else {
            fetchTrack(track.id, isCaching) { url ->
                if (url.status.isSuccess() && url.body != null) {
                    saveFileToDevice(trackName, url.body) { result ->
                        completion(result)
                    }
                } else {
                    completion(ApiResponse.error(url.error!!))
                }
            }
        }
    }

    private fun fetchTrack(
        id: Int,
        isCaching: Boolean,
        completion: (ApiResponse<String>) -> Unit
    ) {
        trackRepo.fetchTrack(id, isCaching) { response ->
            if (response.status.isSuccess() && response.body != null) {
                storageRepo.fetchStorage(response.body.storageDir!!) { storage ->
                    if (storage.status.isSuccess() && storage.body != null) {
                        val url = audioRepo.fetchAudioUrl(storage.body)
                        completion(ApiResponse.success(url))
                    } else {
                        completion(ApiResponse.error(storage.error!!))
                    }
                }
            }
        }
    }

    private fun saveFileToDevice(
        trackName: String,
        url: String,
        completion: (ApiResponse<Uri>) -> Unit
    ) {
        fileRepo.downloadTrack(trackName, url) { response ->
            if (response.isSuccessful && response.body() != null) {
                completion(ApiResponse.success(response.body()))
            } else {
                completion(ApiResponse.error(response.message()))
            }
        }
    }

}
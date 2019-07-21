package com.epam.listento.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import com.epam.listento.api.ApiResponse
import com.epam.listento.repository.AudioRepository
import com.epam.listento.repository.FileRepository
import com.epam.listento.repository.StorageRepository
import com.epam.listento.repository.TrackRepository
import com.epam.listento.utils.ContextProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.URL
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

    private val metadataBuilder = MediaMetadataCompat.Builder()

    fun fillMetadata(track: Track, completion: (MediaMetadataCompat) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            val url = URL(track.album?.albumCover)
            val bitmap = downloadBitmap(url)
            val metadata = metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist?.name)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, track.album?.albumCover)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                .build()
            withContext(Dispatchers.Main) {
                completion(metadata)
            }
        }
    }

    private fun downloadBitmap(url: URL): Bitmap? {
        return try {
            BitmapFactory.decodeStream(url.openStream())
        } catch (e: Exception) {
            null
        }
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
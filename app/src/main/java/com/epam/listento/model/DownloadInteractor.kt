package com.epam.listento.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import androidx.preference.PreferenceManager
import com.epam.listento.R
import com.epam.listento.api.ApiResponse
import com.epam.listento.model.player.utils.*
import com.epam.listento.repository.global.AudioRepository
import com.epam.listento.repository.global.FileRepository
import com.epam.listento.repository.global.StorageRepository
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.utils.ContextProvider
import kotlinx.coroutines.*
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

    private var fetchJob: Job? = null
    private var downloadJob: Job? = null

    private val metadataBuilder = MediaMetadataCompat.Builder()

    fun fillMetadata(track: MediaMetadataCompat, completion: (MediaMetadataCompat) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            val url = URL(track.albumCover)
            val bitmap = downloadBitmap(url)
            val metadata = metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, track.albumCover)
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
        track: MediaMetadataCompat,
        completion: (ApiResponse<Uri>) -> Unit
    ) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(contextProvider.context())
        val isCaching = prefs.getBoolean(contextProvider.getString(R.string.default_caching_key), false)
        val trackName = "${track.artist}-${track.title}.mp3"
        if (trackRepo.checkTrackExistence(trackName)) {
            val uri = trackRepo.fetchTrackUri(trackName)
            if (isCaching) {
                fetchTrack(track.id.toInt(), isCaching) {}
            }
            completion(ApiResponse.success(uri))
        } else {
            fetchTrack(track.id.toInt(), isCaching) { url ->
                if (url.status.isSuccess() && url.body != null) {
                    downloadFile(trackName, url.body) { result ->
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
        completion: suspend (ApiResponse<String>) -> Unit
    ) {
        fetchJob?.cancel()
        fetchJob = GlobalScope.launch(Dispatchers.IO) {
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
    }

    private fun downloadFile(
        trackName: String,
        url: String,
        completion: (ApiResponse<Uri>) -> Unit
    ) {
        downloadJob?.cancel()
        downloadJob = GlobalScope.launch(Dispatchers.IO) {
            fileRepo.downloadTrack(trackName, url) { response ->
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        completion(ApiResponse.success(response.body()))
                    } else {
                        completion(ApiResponse.error(response.message()))
                    }
                }
            }
        }
    }
}

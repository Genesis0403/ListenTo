package com.epam.listento.model

import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.epam.listento.R
import com.epam.listento.api.ApiResponse
import com.epam.listento.model.player.utils.albumCover
import com.epam.listento.model.player.utils.artist
import com.epam.listento.model.player.utils.duration
import com.epam.listento.model.player.utils.id
import com.epam.listento.model.player.utils.title
import com.epam.listento.repository.global.AudioRepository
import com.epam.listento.repository.global.FileRepository
import com.epam.listento.repository.global.StorageRepository
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.utils.ContextProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DownloadInteractor @Inject constructor(
    private val contextProvider: ContextProvider,
    private val audioRepo: AudioRepository,
    private val storageRepo: StorageRepository,
    private val fileRepo: FileRepository,
    private val trackRepo: TrackRepository,
    private val cacheInteractor: CacheInteractor
) {

    private companion object {
        private const val FAILED_TO_LOAD_TRACK = "Failed to load track"
        private const val WIDTH = 320
        private const val HEIGHT = 320
    }

    private val metadataBuilder = MediaMetadataCompat.Builder()
    private var fetchJob: Job? = null
    private var downloadJob: Job? = null

    fun downloadTrack(
        track: MediaMetadataCompat,
        isCaching: Boolean,
        completion: (ApiResponse<Uri>) -> Unit
    ) {
        val trackName = "${track.artist}-${track.title}.mp3"
        if (trackRepo.checkTrackExistence(trackName)) {
            val uri = trackRepo.fetchTrackUri(trackName)
            cacheTrack(track, isCaching)
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

    fun downloadTrack(
        id: Int,
        title: String,
        artist: String,
        completion: (ApiResponse<Uri>) -> Unit
    ) {
        val trackName = "$artist-$title.mp3"
        if (trackRepo.checkTrackExistence(trackName)) {
            val uri = trackRepo.fetchTrackUri(trackName)
            cacheInteractor.cacheTrack(id) {}
            completion(ApiResponse.success(uri))
        } else {
            fetchTrack(id, true) { url ->
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

    fun isCaching(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(contextProvider.context())
        return prefs.getBoolean(contextProvider.getString(R.string.default_caching_key), false)
    }

    private fun cacheTrack(track: MediaMetadataCompat, isCaching: Boolean) {
        cacheInteractor.isTrackInCache(track.id.toInt()) { isInCache ->
            if (isCaching && !isInCache) {
                fetchTrack(track.id.toInt(), isCaching) {}
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

    fun downloadFile(
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

    fun fillMetadata(track: MediaMetadataCompat, completion: (MediaMetadataCompat) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            val bitmap = downloadBitmap(track.albumCover)
            val metadata = metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id)
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

    private fun downloadBitmap(url: String): Bitmap? {
        return Glide.with(contextProvider.context())
            .asBitmap()
            .load(url)
            .fallback(R.drawable.no_photo_24dp)
            .error(R.drawable.no_photo_24dp)
            .submit(WIDTH, HEIGHT)
            .get() // TODO add connectivity manager and load drawable instead image
    }
}

package com.epam.listento.model

import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
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
import javax.inject.Inject

/**
 * TODO refactor this something...
 */
class DownloadInteractor @Inject constructor(
    private val contextProvider: ContextProvider,
    private val audioRepo: AudioRepository,
    private val storageRepo: StorageRepository,
    private val fileRepo: FileRepository,
    private val trackRepo: TrackRepository,
    private val cacheInteractor: CacheInteractor
) {

    private val metadataBuilder = MediaMetadataCompat.Builder()

    @WorkerThread
    suspend fun downloadTrack(
        track: MediaMetadataCompat,
        isCaching: Boolean
    ): ApiResponse<String> {
        val trackName = "${track.artist}-${track.title}.mp3"
        return if (trackRepo.checkTrackExistence(trackName)) {
            val filePath = trackRepo.fetchTrackPath(trackName)
            cacheTrack(track, isCaching)
            ApiResponse.success(filePath)
        } else {
            val url = fetchTrack(track.id.toInt(), isCaching)
            if (url.status.isSuccess() && url.body != null) {
                downloadFile(trackName, url.body)
            } else {
                ApiResponse.error(url.error)
            }
        }
    }

    @WorkerThread
    suspend fun downloadTrack(
        id: Int,
        title: String,
        artist: String
    ): ApiResponse<String> {
        val trackName = "$artist-$title.mp3"
        return if (trackRepo.checkTrackExistence(trackName)) {
            val trackPath = trackRepo.fetchTrackPath(trackName)
            cacheInteractor.cacheTrack(id)
            ApiResponse.success(trackPath)
        } else {
            val url = fetchTrack(id, true)
            if (url.status.isSuccess() && url.body != null) {
                downloadFile(trackName, url.body)
            } else {
                ApiResponse.error(url.error)
            }
        }
    }

    @AnyThread
    fun isCaching(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(contextProvider.context())
        return prefs.getBoolean(contextProvider.getString(R.string.default_caching_key), false)
    }

    @WorkerThread
    private suspend fun cacheTrack(track: MediaMetadataCompat, isCaching: Boolean) {
        val isInCache = cacheInteractor.isTrackInCache(track.id.toInt())
        if (isCaching && !isInCache) {
            fetchTrack(track.id.toInt(), isCaching)
        }
    }

    @WorkerThread
    private suspend fun fetchTrack(
        id: Int,
        isCaching: Boolean
    ): ApiResponse<String> {
        return trackRepo.fetchTrack(id, isCaching).run {
            if (status.isSuccess() && body != null) {
                storageRepo.fetchStorage(body.storageDir!!).run {
                    if (status.isSuccess() && body != null) {
                        val url = audioRepo.fetchAudioUrl(body)
                        ApiResponse.success(url)
                    } else {
                        ApiResponse.error(error)
                    }
                }
            } else {
                ApiResponse.error(DOWNLOAD_ERROR)
            }
        }
    }

    @WorkerThread
    suspend fun downloadFile(
        trackName: String,
        url: String
    ): ApiResponse<String> {
        return fileRepo.downloadTrack(trackName, url).run {
            if (status.isSuccess() && body != null) {
                ApiResponse.success(body.toString())
            } else {
                ApiResponse.error(error)
            }
        }
    }

    @WorkerThread
    suspend fun fillMetadata(track: MediaMetadataCompat): MediaMetadataCompat {
        val bitmap = downloadBitmap(track.albumCover)
        return metadataBuilder
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, track.id)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, track.albumCover)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
            .build()
    }

    @WorkerThread
    private suspend fun downloadBitmap(url: String): Bitmap? {
        return Glide.with(contextProvider.context())
            .asBitmap()
            .load(url)
            .fallback(R.drawable.no_photo_24dp)
            .error(R.drawable.no_photo_24dp)
            .submit(WIDTH, HEIGHT)
            .get() // TODO add connectivity manager and load drawable instead image
    }

    private companion object {
        private const val WIDTH = 320
        private const val HEIGHT = 320
        private const val DOWNLOAD_ERROR = "An error occurred during downloading."
    }
}

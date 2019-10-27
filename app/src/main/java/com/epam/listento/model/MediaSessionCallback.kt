package com.epam.listento.model

import android.content.Context
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.epam.listento.R
import com.epam.listento.model.player.utils.EMPTY_DURATION
import com.epam.listento.model.player.utils.EMPTY_ID
import com.epam.listento.model.player.utils.UNKNOWN
import com.epam.listento.model.player.utils.id
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.AppDispatchers
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MediaSessionCallback(
    dispatchers: AppDispatchers,
    private val context: Context,
    private val musicRepo: MusicRepository,
    private val downloadInteractor: DownloadInteractor,
    private val player: SimpleExoPlayer,
    private val onSessionUpdate: (metadata: MediaMetadataCompat?, isActive: Boolean, state: Int) -> Unit
) : MediaSessionCompat.Callback() {

    private val sessionScope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private var sessionJob: Job? = null

    private val emptyMetadata = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, EMPTY_ID)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, UNKNOWN)
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, UNKNOWN)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, EMPTY_DURATION)
        .build()

    private var currentState = PlaybackStateCompat.STATE_STOPPED
        @Synchronized set

    private var currentPlaying: MediaMetadataCompat? = null

    override fun onPlay() {
        super.onPlay()
        sessionJob?.cancel()

        if (player.playWhenReady) {
            player.playWhenReady = false
        }

        val track = musicRepo.getCurrent()

        sessionJob = sessionScope.launch {
            val metadata = downloadInteractor.fillMetadata(track)
            onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING)

            if (currentState == PlaybackStateCompat.STATE_PAUSED && currentPlaying?.id == track.id) {
                player.playWhenReady = true
            } else {
                currentPlaying = metadata
                val isCaching = downloadInteractor.isCaching()
                val result = downloadInteractor.downloadTrack(track, isCaching)
                if (result.status.isSuccess() && result.body != null) {
                    prepareToPlay(Uri.parse(result.body))
                    player.playWhenReady = true
                }
                currentState = PlaybackStateCompat.STATE_PLAYING
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (player.playWhenReady) {
            player.playWhenReady = false
        }
        onSessionUpdate(currentPlaying, true, PlaybackStateCompat.STATE_PAUSED)
        currentState = PlaybackStateCompat.STATE_PAUSED
    }

    override fun onStop() {
        super.onStop()

        if (player.playWhenReady) {
            player.playWhenReady = false
        }
        onSessionUpdate(emptyMetadata, false, PlaybackStateCompat.STATE_STOPPED)

        currentState = PlaybackStateCompat.STATE_STOPPED
        currentPlaying = null
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        sessionJob?.cancel()

        if (player.playWhenReady) {
            player.playWhenReady = false
        }

        val track = musicRepo.getNext()

        sessionJob = sessionScope.launch {
            val metadata = downloadInteractor.fillMetadata(track)
            onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING)
            currentPlaying = metadata

            val isCaching = downloadInteractor.isCaching()
            val result = downloadInteractor.downloadTrack(track, isCaching)
            if (result.status.isSuccess() && result.body != null) {
                prepareToPlay(Uri.parse(result.body))
                player.playWhenReady = true
            }
        }
        currentState = PlaybackStateCompat.STATE_PLAYING
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        sessionJob?.cancel()

        if (player.playWhenReady) {
            player.playWhenReady = false
        }

        val track = musicRepo.getPrevious()

        sessionJob = sessionScope.launch {
            val metadata = downloadInteractor.fillMetadata(track)
            onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING)
            currentPlaying = metadata

            val isCaching = downloadInteractor.isCaching()
            val result = downloadInteractor.downloadTrack(track, isCaching)
            if (result.status.isSuccess() && result.body != null) {
                prepareToPlay(Uri.parse(result.body))
                player.playWhenReady = true
            }
        }
        currentState = PlaybackStateCompat.STATE_PLAYING
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        player.seekTo(pos)
    }

    private fun prepareToPlay(uri: Uri) {
        val dataSourceFactory = DefaultDataSourceFactory(
            context,
            Util.getUserAgent(context, context.getString(R.string.app_name))
        )
        player.prepare(
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
        )
    }
}

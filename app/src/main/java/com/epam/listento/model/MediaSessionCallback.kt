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
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class MediaSessionCallback(
    private val context: Context,
    private val musicRepo: MusicRepository,
    private val downloadInteractor: DownloadInteractor,
    private val player: SimpleExoPlayer,
    private val onSessionUpdate: (metadata: MediaMetadataCompat?, isActive: Boolean, state: Int) -> Unit
) : MediaSessionCompat.Callback() {

    private val emptyMetadata = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, EMPTY_ID)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, UNKNOWN)
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, UNKNOWN)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, EMPTY_DURATION)
        .build()
    private var currentState = PlaybackStateCompat.STATE_STOPPED
    private var currentPlaying: MediaMetadataCompat? = null

    override fun onPlay() {
        super.onPlay()

        if (player.playWhenReady) {
            player.playWhenReady = false
        }

        val track = musicRepo.getCurrent()
        downloadInteractor.fillMetadata(track) { metadata ->
            onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING)

            if (currentState == PlaybackStateCompat.STATE_PAUSED && currentPlaying?.id == track.id) {
                player.playWhenReady = true
            } else {
                currentPlaying = metadata
                val isCaching = downloadInteractor.isCaching()
                downloadInteractor.downloadTrack(track, isCaching) { result ->
                    if (result.status.isSuccess() && result.body != null) {
                        prepareToPlay(result.body)
                        player.playWhenReady = true
                    }
                }
            }
            currentState = PlaybackStateCompat.STATE_PLAYING
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

        if (player.playWhenReady) {
            player.playWhenReady = false
        }

        val track = musicRepo.getNext()

        downloadInteractor.fillMetadata(track) { metadata ->
            onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING)
            currentPlaying = metadata

            val isCaching = downloadInteractor.isCaching()
            downloadInteractor.downloadTrack(track, isCaching) { result ->
                if (result.status.isSuccess() && result.body != null) {
                    prepareToPlay(result.body)
                    player.playWhenReady = true
                }
            }
        }
        currentState = PlaybackStateCompat.STATE_PLAYING
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()

        if (player.playWhenReady) {
            player.playWhenReady = false
        }

        val track = musicRepo.getPrevious()

        downloadInteractor.fillMetadata(track) { metadata ->
            onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING)
            currentPlaying = metadata

            val isCaching = downloadInteractor.isCaching()
            downloadInteractor.downloadTrack(track, isCaching) { result ->
                if (result.status.isSuccess() && result.body != null) {
                    prepareToPlay(result.body)
                    player.playWhenReady = true
                }
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

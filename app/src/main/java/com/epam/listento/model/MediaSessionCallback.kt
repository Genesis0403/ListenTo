package com.epam.listento.model

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.epam.listento.R
import com.epam.listento.repository.MusicRepository
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util

class MediaSessionCallback(
    private val context: Context,
    private val musicRepo: MusicRepository,
    private val downloadInteractor: DownloadInteractor,
    private val player: SimpleExoPlayer,
    private val onSessionUpdate: (MediaMetadataCompat?, Boolean, Int) -> Unit
) : MediaSessionCompat.Callback() {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var isAudioFocused = false
    private val audioFocusRequest: AudioFocusRequest?
    private var currentState = PlaybackStateCompat.STATE_STOPPED

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                isAudioFocused = true
                onPlay()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                onPause()
            }
            else -> {
                isAudioFocused = false
                onPause()
            }
        }
    }

    init {
        audioFocusRequest = initAudioFocusRequest()
    }

    override fun onPlay() {
        super.onPlay()
        context.startService(Intent(context.applicationContext, PlayerService::class.java))

        if (player.playWhenReady) {
            player.playWhenReady = false
        }

        if (!isAudioFocused) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.requestAudioFocus(audioFocusRequest)
            } else {
                audioManager.requestAudioFocus(
                    audioFocusListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        }

        val track = musicRepo.getCurrent()
        downloadInteractor.fillMetadata(track) { metadata ->
            onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING)

            if (currentState != PlaybackStateCompat.STATE_PAUSED) {
                downloadInteractor.downloadTrack(track, true) { result ->
                    if (result.status.isSuccess() && result.body != null) {
                        prepareToPlay(result.body)
                        player.playWhenReady = true
                    }
                }
            } else {
                player.playWhenReady = true
            }
            currentState = PlaybackStateCompat.STATE_PLAYING
        }
    }

    override fun onPause() {
        super.onPause()

        if (player.playWhenReady) {
            player.playWhenReady = false
        }
        onSessionUpdate(null, true, PlaybackStateCompat.STATE_PAUSED)
        currentState = PlaybackStateCompat.STATE_PAUSED

    }

    override fun onStop() {
        super.onStop()

        if (player.playWhenReady) {
            player.playWhenReady = false
        }
        onSessionUpdate(null, false, PlaybackStateCompat.STATE_STOPPED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(audioFocusListener)
        }
        currentState = PlaybackStateCompat.STATE_STOPPED
    }

    override fun onSkipToNext() {
        super.onSkipToNext()

        if (player.playWhenReady) {
            player.playWhenReady = false
        }

        val track = musicRepo.getNext()

        downloadInteractor.fillMetadata(track) { metadata ->
            onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING)

            downloadInteractor.downloadTrack(track, true) { result ->
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

            downloadInteractor.downloadTrack(track, true) { result ->
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

    private fun initAudioFocusRequest(): AudioFocusRequest? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .setAudioAttributes(attributes)
                .build()
        } else {
            null
        }
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

package com.epam.listento.model

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.epam.listento.R
import com.epam.listento.api.model.ApiTrack
import com.epam.listento.repository.MusicRepository
import com.epam.listento.repository.TrackRepository
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.lang.ref.WeakReference
import java.net.URL

class MediaSessionCallback(
    private val context: Context,
    private val musicRepo: MusicRepository,
    private val trackRepo: TrackRepository,
    private val mediaSession: WeakReference<MediaSessionCompat>,
    private val player: SimpleExoPlayer,
    private val stateBuilder: PlaybackStateCompat.Builder,
    private val onComplete: (Int) -> Unit
) : MediaSessionCompat.Callback() {

    private val metadataBuilder = MediaMetadataCompat.Builder()
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var isAudioFocused = false
    private val audioFocusRequest: AudioFocusRequest?

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

        val track = musicRepo.getCurrent()
        trackRepo.fecthTrack(track.id) { response ->
            response.body?.let {
                val metadata = fillMetadataFromTrack(it)
                mediaSession.get()?.setMetadata(metadata)

                if (mediaSession.get()?.controller?.playbackState?.state != PlaybackStateCompat.STATE_PAUSED) {
                    downloadTrack(it)
                } else {
                    player.playWhenReady = true
                }

                updateSessionData(true, PlaybackStateCompat.STATE_PLAYING)
                onComplete(PlaybackStateCompat.STATE_PLAYING)
            }
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
    }

    override fun onPause() {
        super.onPause()
        if (player.playWhenReady) {
            player.playWhenReady = false
        }
        updateSessionData(true, PlaybackStateCompat.STATE_PAUSED)
        onComplete(PlaybackStateCompat.STATE_PAUSED)
    }

    override fun onStop() {
        super.onStop()
        if (player.playWhenReady) {
            player.playWhenReady = false
        }
        updateSessionData(false, PlaybackStateCompat.STATE_STOPPED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(audioFocusListener)
        }
        onComplete(PlaybackStateCompat.STATE_STOPPED)
    }

    override fun onSkipToNext() {
        super.onSkipToNext()

        val track = musicRepo.getNext()
        trackRepo.fecthTrack(track.id) { response ->
            response.body?.let {
                val metadata = fillMetadataFromTrack(it)
                mediaSession.get()?.setMetadata(metadata)
                downloadTrack(it)
                updateSessionData(true, PlaybackStateCompat.STATE_PLAYING)
                onComplete(PlaybackStateCompat.STATE_PLAYING)
            }
        }
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()

        val track = musicRepo.getPrevious()
        trackRepo.fecthTrack(track.id) { response ->
            response.body?.let {
                val metadata = fillMetadataFromTrack(it)
                mediaSession.get()?.setMetadata(metadata)

                downloadTrack(it)
                updateSessionData(true, PlaybackStateCompat.STATE_PLAYING)
                onComplete(PlaybackStateCompat.STATE_PLAYING)
            }
        }
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

    private fun fillMetadataFromTrack(track: NotificationTrack): MediaMetadataCompat {
        return metadataBuilder
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.duration)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, track.coverBitmap)
            .build()
    }

    private fun downloadTrack(track: NotificationTrack) {
        musicRepo.downloadTrack(track) { response ->
            if (response.status.isSuccess() && response.body != null) {
                prepareToPlay(response.body)
                player.playWhenReady = true
            }
        }
    }

    private fun updateSessionData(isActive: Boolean, state: Int) {
        mediaSession.get()?.let {
            it.isActive = isActive
            it.setPlaybackState(
                stateBuilder.setState(
                    state,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1f
                ).build()
            )
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

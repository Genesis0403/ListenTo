package com.epam.listento.model.player

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MediaSessionManager(context: Context, token: MediaSessionCompat.Token) {

    companion object {
        @Volatile
        private var instance: MediaSessionManager? = null

        fun getInstance(
            context: Context,
            token: MediaSessionCompat.Token
        ): MediaSessionManager {
            return instance ?: synchronized(this) {
                instance ?: MediaSessionManager(context, token).also {
                    instance = it
                }
            }
        }
    }

    private val _currentPlaying = MutableLiveData<MediaMetadataCompat>()
    val currentPlaying: LiveData<MediaMetadataCompat>
        get() = _currentPlaying

    private val _isPlaying = MutableLiveData<PlaybackState>()
    val isPlaying: LiveData<PlaybackState>
        get() = _isPlaying

    private val callback = ControllerCallback()
    val controller: MediaControllerCompat = MediaControllerCompat(context, token)

    val transportControls: MediaControllerCompat.TransportControls
        get() = controller.transportControls

    init {
        controller.registerCallback(callback)
    }

    private inner class ControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            val playback = when (state?.state) {
                PlaybackStateCompat.STATE_PLAYING -> PlaybackState.PLAYING
                PlaybackStateCompat.STATE_PAUSED -> PlaybackState.PAUSED
                PlaybackStateCompat.STATE_STOPPED -> PlaybackState.STOPPED
                else -> PlaybackState.UNKNOWN
            }
            _isPlaying.postValue(playback)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            _currentPlaying.postValue(metadata)
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            controller.unregisterCallback(callback)
            instance = null
        }
    }
}
package com.epam.listento

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.epam.listento.model.PlayerService
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.player.utils.id
import com.epam.listento.utils.ContextProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceHelper @Inject constructor(
    private val contextProvider: ContextProvider
) {

    private val _playbackState = MutableLiveData<PlaybackState>()
    val playbackState: LiveData<PlaybackState> get() = _playbackState

    private val _currentPlaying = MutableLiveData<Int>()
    val currentPlaying: LiveData<Int> get() = _currentPlaying

    private var binder: PlayerService.PlayerBinder? = null
    private var controller: MediaControllerCompat? = null

    val progressMs: Long? get() = binder?.getProgress()

    val transportControls by lazy(LazyThreadSafetyMode.NONE) {
        controller?.transportControls
    }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            controller = null
            binder = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as PlayerService.PlayerBinder
            binder?.let { binder ->
                val token = binder.getSessionToken() ?: return
                try {
                    controller = MediaControllerCompat(contextProvider.context(), token).also {
                        it.registerCallback(callback)
                    }
                } catch (e: Exception) {
                    controller = null
                }
            }
        }
    }

    private val callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            Log.d(TAG, "PLAYBACK")
            _playbackState.value = getPlaybackState(state?.state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            Log.d(TAG, "METADATA")
            _currentPlaying.value = metadata?.id?.toInt() ?: -1
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            Log.d(TAG, "SESSION DESTROYED")
            controller?.unregisterCallback(this)
            controller = null
        }
    }

    fun subscribe() {
        Log.d(TAG, "STARTING SERVICE VIA SERVICE HELPER")
        val context = contextProvider.context()
        context.bindService(
            Intent(context, PlayerService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun unsubscribe() {
        contextProvider.context().unbindService(connection)
        binder = null
    }

    private fun getPlaybackState(state: Int?): PlaybackState {
        return when (state) {
            PlaybackStateCompat.STATE_PLAYING -> PlaybackState.Playing
            PlaybackStateCompat.STATE_PAUSED -> PlaybackState.Paused
            PlaybackStateCompat.STATE_STOPPED -> PlaybackState.Stopped
            else -> PlaybackState.None
        }
    }

    private companion object {
        private const val TAG = "ServiceHelper"
    }
}

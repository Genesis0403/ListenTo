package com.epam.listento.model

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.epam.listento.App
import com.epam.listento.model.player.NOTIFICATION_ID
import com.epam.listento.model.player.NotificationBuilder
import com.epam.listento.model.player.STOP_ACTION
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.ui.MainActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import javax.inject.Inject

class PlayerService : Service() {

    private companion object {
        private const val TAG = "PLAYER_SERVICE"
        private const val DOES_HANDLE_AUDIO_FOCUS = true
    }

    @Inject
    lateinit var musicRepo: MusicRepository

    @Inject
    lateinit var notificationBuilder: NotificationBuilder

    @Inject
    lateinit var downloadInteractor: DownloadInteractor

    @Inject
    lateinit var player: SimpleExoPlayer

    private lateinit var mediaSession: MediaSessionCompat
    lateinit var controller: MediaControllerCompat

    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private var mediaSessionCallback: MediaSessionCallback? = null

    private val activityIntent by lazy { Intent(applicationContext, MainActivity::class.java) }

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private var isForeground = false
    private val callback = ControllerCallback()

    override fun onBind(intent: Intent?): IBinder? {
        return PlayerBinder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == STOP_ACTION) {
                controller.transportControls.stop()
            } else {
                MediaButtonReceiver.handleIntent(mediaSession, intent)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        App.component.inject(this)
        super.onCreate()

        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()

        player.run {
            addListener(playerListener)
            setAudioAttributes(attributes, DOES_HANDLE_AUDIO_FOCUS)
        }

        stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_STOP
            )

        mediaSession = initMediaSession()

        mediaSessionCallback = MediaSessionCallback(
            this,
            musicRepo,
            downloadInteractor,
            player
        ) { metadata, isActive, state ->
            mediaSession.setMetadata(metadata)
            updateSessionData(isActive, state)
        }

        mediaSession.setCallback(mediaSessionCallback)

        controller = MediaControllerCompat(this, mediaSession).also {
            it.registerCallback(callback)
        }
    }

    private fun updateSessionData(isActive: Boolean, state: Int) {
        mediaSession.run {
            this.isActive = isActive
            setPlaybackState(
                stateBuilder.setState(
                    state,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1f
                ).build()
            )
        }
    }

    private fun initMediaSession(): MediaSessionCompat {
        return MediaSessionCompat(this, TAG).apply {
            setPlaybackState(stateBuilder.build())
            setSessionActivity(
                PendingIntent.getActivity(applicationContext, 0, activityIntent, 0)
            )
            setMediaButtonReceiver(
                PendingIntent.getBroadcast(
                    applicationContext,
                    0,
                    Intent(
                        Intent.ACTION_MEDIA_BUTTON,
                        null,
                        applicationContext,
                        MediaButtonReceiver::class.java
                    ).also {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    },
                    0
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ON DESTROY")
        controller.unregisterCallback(callback)
        mediaSession.isActive = false
        mediaSession.setCallback(null)
        mediaSession.release()
//        //player.release()
    }

    inner class PlayerBinder : Binder() {
        fun getSessionToken(): MediaSessionCompat.Token? = mediaSession.sessionToken

        fun getProgress(): Long = player.currentPosition
    }

    private inner class ControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            state?.let { updateNotification(it) }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            controller.playbackState?.let { updateNotification(it) }
        }
    }

    private fun updateNotification(state: PlaybackStateCompat) {

        val notification: Notification? = if (controller.metadata != null) {
            mediaSession.let { notificationBuilder.buildNotification(it.sessionToken) }
        } else {
            null
        }

        when (state.state) {
            PlaybackStateCompat.STATE_PLAYING,
            PlaybackStateCompat.STATE_BUFFERING -> {

                registerReceiver(
                    becomeNoisyReceiver,
                    IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                )

                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification)
                    if (!isForeground) {
                        ContextCompat.startForegroundService(
                            applicationContext,
                            Intent(this, PlayerService::class.java)
                        )
                        startForeground(NOTIFICATION_ID, notification)
                        isForeground = true
                    }
                }
            }
            else -> {
                try {
                    unregisterReceiver(becomeNoisyReceiver)
                } catch (e: Exception) {
                }

                if (isForeground) {
                    stopForeground(false)
                    isForeground = false

                    if (notification != null) {
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    } else {
                        stopForeground(true)
                    }
                }
                if (state.state == PlaybackStateCompat.STATE_STOPPED) {
                    stopSelf()
                }
            }
        }
    }

    private val becomeNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    controller.transportControls.pause()
                }
                else -> return
            }
        }
    }

    private val playerListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            super.onPlayerStateChanged(playWhenReady, playbackState)
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                controller.transportControls.skipToNext()
            }
        }
    }
}

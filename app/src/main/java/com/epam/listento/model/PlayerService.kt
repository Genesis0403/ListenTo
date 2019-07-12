package com.epam.listento.model

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.repository.MusicRepository
import com.epam.listento.repository.TrackRepository
import com.epam.listento.ui.MainActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import java.lang.ref.WeakReference
import javax.inject.Inject

class PlayerService : Service() {

    private companion object {
        private const val TAG = "PLAYER_SERVICE"
        private const val CHANNEL_ID = "LISTEN_TO_ID"
        private const val NOTIFICATION_ID = 1
    }

    @Inject
    lateinit var musicRepo: MusicRepository

    @Inject
    lateinit var trackRepo: TrackRepository

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaSessionCallback: MediaSessionCallback

    private val channelId by lazy { createNotificationChannel() }
    private val activityIntent by lazy { Intent(applicationContext, MainActivity::class.java) }
    private var currentState = PlaybackStateCompat.STATE_STOPPED

    override fun onBind(intent: Intent?): IBinder? {
        return PlayerBinder()
    }

    override fun onCreate() {
        super.onCreate()
        App.component.inject(this)

        stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_STOP
            )

        player = ExoPlayerFactory.newSimpleInstance(this).also {
            it.addListener(playerListener)
        }
        mediaSession = initMediaSession()

        mediaSessionCallback = MediaSessionCallback(
            applicationContext,
            musicRepo,
            trackRepo,
            WeakReference(mediaSession),
            player,
            stateBuilder
        ) { state ->
            currentState = state
            refreshNotification()
            if (state == PlaybackStateCompat.STATE_STOPPED) {
                stopSelf()
            }
        }

        mediaSession.setCallback(mediaSessionCallback)

        registerReceiver(becomeNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
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
                        Intent.ACTION_MEDIA_BUTTON, null, applicationContext, MediaButtonReceiver::class.java
                    ),
                    0
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ON DESTROY")
        unregisterReceiver(becomeNoisyReceiver)
        mediaSession.release()
        player.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotification(): Notification { // TODO move to another class?
        return NotificationCompat.Builder(this, channelId).apply {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.notification_small_icon_24dp)
            priority = NotificationCompat.PRIORITY_HIGH
            setShowWhen(false)
            setOnlyAlertOnce(true)
            color = ContextCompat.getColor(this@PlayerService, R.color.colorPrimary)

            addAction(
                R.drawable.ic_fast_rewind_black_24dp,
                getString(R.string.previous_playback),
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            if (currentState == PlaybackStateCompat.STATE_PLAYING) {
                addAction(
                    R.drawable.ic_pause_black_24dp,
                    getString(R.string.play_action),
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            } else {
                addAction(
                    R.drawable.ic_play_arrow_black_24dp,
                    getString(R.string.pause_action),
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            }
            addAction(
                R.drawable.ic_fast_forward_black_24dp,
                getString(R.string.next_action),
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            )
            setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession.sessionToken)
            )
            fillNotificationWithMetadata(this)
        }.build()
    }

    private fun createNotificationChannel(): String {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) ""
        else {
            val channelName = getString(R.string.notification_channel_name)
            val channel = NotificationChannel(CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH)
            channel.description = getString(R.string.notification_channel_description)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            CHANNEL_ID
        }
    }

    private fun NotificationCompat.Builder.addAction(
        icon: Int,
        title: String,
        state: Long
    ): NotificationCompat.Builder {
        return this.addAction(
            NotificationCompat.Action(
                icon,
                title,
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    applicationContext,
                    state
                )
            )
        )
    }

    private fun fillNotificationWithMetadata(builder: NotificationCompat.Builder): NotificationCompat.Builder {
        val metadata = mediaSession.controller.metadata.description
        return builder.setContentTitle(metadata.title)
            .setContentText(metadata.subtitle)
            .setLargeIcon(metadata.iconBitmap)
            .setContentIntent(mediaSession.controller.sessionActivity)
            .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(applicationContext, PlaybackStateCompat.ACTION_STOP)
            )
    }

    private fun refreshNotification() {
        when (currentState) {
            PlaybackStateCompat.STATE_PLAYING -> {
                startForeground(NOTIFICATION_ID, createNotification())
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, createNotification())
                stopForeground(false)
            }
            else -> stopForeground(true)
        }
    }

    inner class PlayerBinder : Binder() {
        fun getSessionToken(): MediaSessionCompat.Token? = mediaSession.sessionToken

        fun changeSourceData(data: List<Track>) {
            if (musicRepo.isDataChanged(data)) {
                musicRepo.setSource(data)
            }
        }

        fun playTrack(track: Track) {
            musicRepo.setCurrent(track)
            mediaSession.controller.transportControls.play()
        }
    }

    private val becomeNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    mediaSessionCallback.onPause()
                }
                else -> return
            }
        }
    }

    private val playerListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            super.onPlayerStateChanged(playWhenReady, playbackState)
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                mediaSessionCallback.onSkipToNext()
            }
        }
    }
}

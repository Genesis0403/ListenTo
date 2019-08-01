package com.epam.listento.model.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.epam.listento.R
import com.epam.listento.model.PlayerService
import com.epam.listento.model.player.utils.isPlaying
import com.epam.listento.ui.PlayerActivity
import com.epam.listento.utils.ContextProvider
import javax.inject.Inject

const val CHANNEL_ID = "LISTEN_TO_ID"
const val NOTIFICATION_ID = 1
const val IS_ALARM_ONCE = true
const val IS_SHOW_WHEN = false
const val STOP_ACTION = "com.epam.listento.STOP_ACTION"

class NotificationBuilder @Inject constructor(
    private val contextProvider: ContextProvider
) {

    private val notificationManager: NotificationManager =
        contextProvider.context().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val actionPlay = NotificationCompat.Action(
        R.drawable.exo_controls_play,
        contextProvider.context().getString(R.string.play_action),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            contextProvider.context(),
            PlaybackStateCompat.ACTION_PLAY
        )
    )

    private val actionPause = NotificationCompat.Action(
        R.drawable.exo_controls_pause,
        contextProvider.context().getString(R.string.pause_action),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            contextProvider.context(),
            PlaybackStateCompat.ACTION_PAUSE
        )
    )

    private val actionSkipToNext = NotificationCompat.Action(
        R.drawable.exo_controls_fastforward,
        contextProvider.context().getString(R.string.next_action),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            contextProvider.context(),
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
    )

    private val actionSkipToPrev = NotificationCompat.Action(
        R.drawable.exo_controls_rewind,
        contextProvider.context().getString(R.string.previous_action),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            contextProvider.context(),
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    )

    private val stopIntent =
        PendingIntent.getService(
            contextProvider.context(),
            0,
            Intent(contextProvider.context(), PlayerService::class.java).also {
                it.action = STOP_ACTION
            },
            0
        )

    private val contentIntent = PendingIntent.getActivity(
        contextProvider.context(),
        0,
        Intent(contextProvider.context(), PlayerActivity::class.java),
        0
    )

    fun buildNotification(token: MediaSessionCompat.Token): Notification {
        createChannel()

        val context = contextProvider.context()
        val controller = MediaControllerCompat(context, token)
        val metadata = controller.metadata
        val description = metadata.description

        val mediaStyle = MediaStyle()
            .setMediaSession(token)
            .setShowActionsInCompactView(0, 1, 2)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)

        builder.apply {
            addAction(actionSkipToPrev)
            if (controller.playbackState.isPlaying) {
                addAction(actionPause)
            } else {
                addAction(actionPlay)
            }
            addAction(actionSkipToNext)
        }

        return builder.setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setLargeIcon(description.iconBitmap)
            .setSmallIcon(R.drawable.notification_small_icon_24dp)
            .setOnlyAlertOnce(IS_ALARM_ONCE)
            .setStyle(mediaStyle)
            .setContentIntent(contentIntent)
            .setDeleteIntent(stopIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(IS_SHOW_WHEN)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val context = contextProvider.context()
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(
                R.string.notification_channel_name
            ),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }

        notificationManager.createNotificationChannel(channel)
    }
}
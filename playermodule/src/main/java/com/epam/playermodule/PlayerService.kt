package com.epam.playermodule

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.MediaBrowserServiceCompat

private const val MEDIA_ROOT_ID = "LISTEN_TO_MEDIA_ROOT_ID"
private const val TAG = "PLAYER_SERVICE"
private const val REQUEST_CODE = 1337

@Deprecated("Not used. See PlayerService.kt at ListenTo")
class PlayerService : MediaBrowserServiceCompat() {

    private var mediaSession: MediaSessionCompat? = null
    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    override fun onCreate() {
        super.onCreate()

        mediaSession = MediaSessionCompat(baseContext, TAG).apply {

            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY
                            or PlaybackStateCompat.ACTION_PLAY_PAUSE
                            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            setPlaybackState(stateBuilder.build())

            //TODO set callbacks and sessionToken

        }
    }

    private fun provideSessionToken(): PendingIntent {
        return PendingIntent.getService(
            baseContext,
            REQUEST_CODE,
            Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

}

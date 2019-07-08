package com.epam.playermodule

import android.support.v4.media.session.MediaSessionCompat

class MediaSession(
) : MediaSessionCompat.Callback() {

    override fun onPlay() {
        super.onPlay()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
    }
}
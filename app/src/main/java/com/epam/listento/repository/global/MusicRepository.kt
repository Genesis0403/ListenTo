package com.epam.listento.repository.global

import android.support.v4.media.MediaMetadataCompat

interface MusicRepository {
    fun isDataChanged(data: List<MediaMetadataCompat>): Boolean
    fun getCurrent(): MediaMetadataCompat
    fun getNext(): MediaMetadataCompat
    fun getPrevious(): MediaMetadataCompat
    fun setSource(data: List<MediaMetadataCompat>)
    fun setCurrent(track: MediaMetadataCompat)
    fun containsTrack(track: MediaMetadataCompat): Boolean
}

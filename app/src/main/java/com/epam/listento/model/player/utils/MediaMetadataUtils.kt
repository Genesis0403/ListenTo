package com.epam.listento.model.player.utils

import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat

const val UNKNOWN = "<unknown>"
const val EMPTY_ID = "-1"
const val EMPTY_DURATION = 0L

inline val MediaMetadataCompat.id: String
    get() = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)

inline val MediaMetadataCompat.artist: String
    get() = getString(MediaMetadataCompat.METADATA_KEY_ARTIST)

inline val MediaMetadataCompat.title: String
    get() = getString(MediaMetadataCompat.METADATA_KEY_TITLE)

inline val MediaMetadataCompat.albumCover: String
    get() = getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)

inline val MediaMetadataCompat.bitmap: Bitmap?
    get() = getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)

inline val MediaMetadataCompat.duration: Long
    get() = getLong(MediaMetadataCompat.METADATA_KEY_DURATION)

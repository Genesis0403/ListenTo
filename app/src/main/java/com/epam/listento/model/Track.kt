package com.epam.listento.model

import android.support.v4.media.MediaMetadataCompat
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity
data class Track(
    @PrimaryKey
    val id: Int,
    val duration: Long,
    val title: String,
    val artist: Artist?,
    val storageDir: String,
    val album: Album?
)

fun Track.durationToString(): String {
    return String.format(
        "%d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(duration) % TimeUnit.HOURS.toMinutes(1),
        TimeUnit.MILLISECONDS.toSeconds(duration) % TimeUnit.MINUTES.toSeconds(1)
    )
}

fun Track.toMetadata(): MediaMetadataCompat {
    val cover = album?.albumCover ?: artist?.coverUrl
    return MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id.toString())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist?.name)
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, cover)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
        .build()
}

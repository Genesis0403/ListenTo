package com.epam.listento.model

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

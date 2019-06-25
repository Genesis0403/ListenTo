package com.epam.listento.model

data class Track(
    val id: Int,
    val duration: String,
    val title: String,
    val artist: Artist?,
    val storageDir: String,
    val album: Album?
)
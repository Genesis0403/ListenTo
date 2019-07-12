package com.epam.listento.model

data class Album(
    val id: Int,
    val thumbnailCover: String,
    val albumCover: String,
    val title: String,
    val artist: Artist?
)

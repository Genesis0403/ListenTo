package com.epam.listento.model

data class NotificationTrack(
    val id: Int,
    val storageDir: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val cover: String
)

package com.epam.listento.model

import android.graphics.Bitmap

data class NotificationTrack(
    val id: Int,
    val storageDir: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val coverBitmap: Bitmap
)
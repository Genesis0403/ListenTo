package com.epam.listento.domain

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity
data class DomainTrack(
    @PrimaryKey
    val id: Int,
    val storageDir: String?,
    val durationMs: Long?,
    val title: String?,
    val albums: List<DomainAlbum>?,
    val artists: List<DomainArtist>?,
    var timestamp: Date? = null
)

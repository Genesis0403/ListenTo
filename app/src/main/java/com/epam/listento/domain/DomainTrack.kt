package com.epam.listento.domain

data class DomainTrack(
    val id: Int,
    val storageDir: String?,
    val durationMs: Long?,
    val title: String?,
    val albums: List<DomainAlbum>?,
    val artists: List<DomainArtist>?
)

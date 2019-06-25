package com.epam.listento.domain

data class DomainAlbum(
    val id: Int,
    val coverUri: String?,
    val storageDir: String?,
    val title: String?,
    val artists: List<DomainArtist>?
)

package com.epam.listento.domain

data class DomainArtist(
    val id: Int,
    val uri: String?,
    val name: String?,
    val cover: DomainCover?
)

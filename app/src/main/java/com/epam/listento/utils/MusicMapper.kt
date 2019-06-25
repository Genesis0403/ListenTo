package com.epam.listento.utils

import com.epam.listento.api.model.*
import com.epam.listento.domain.*

interface MusicMapper {
    fun trackToDomain(track: ApiTrack): DomainTrack
    fun storageToDomain(storage: ApiStorage): DomainStorage
    fun artistToDomain(artist: ApiArtist): DomainArtist
    fun albumToDomain(album: ApiAlbum): DomainAlbum
    fun coverToDomain(cover: ApiCover): DomainCover
}
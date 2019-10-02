package com.epam.listento.utils

import com.epam.listento.api.model.ApiAlbum
import com.epam.listento.api.model.ApiArtist
import com.epam.listento.api.model.ApiCover
import com.epam.listento.api.model.ApiStorage
import com.epam.listento.api.model.ApiTrack
import com.epam.listento.domain.DomainAlbum
import com.epam.listento.domain.DomainArtist
import com.epam.listento.domain.DomainCover
import com.epam.listento.domain.DomainStorage
import com.epam.listento.domain.DomainTrack

interface MusicMapper {
    fun trackToDomain(track: ApiTrack): DomainTrack
    fun storageToDomain(storage: ApiStorage): DomainStorage
    fun artistToDomain(artist: ApiArtist): DomainArtist
    fun albumToDomain(album: ApiAlbum): DomainAlbum
    fun coverToDomain(cover: ApiCover): DomainCover
}
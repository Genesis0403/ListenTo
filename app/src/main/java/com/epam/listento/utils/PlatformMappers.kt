package com.epam.listento.utils

import com.epam.listento.domain.DomainAlbum
import com.epam.listento.domain.DomainArtist
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Album
import com.epam.listento.model.Artist
import com.epam.listento.model.Track

interface PlatformMappers {
    fun mapTrack(track: DomainTrack?): Track?
    fun mapAlbum(album: DomainAlbum?): Album?
    fun mapArtist(artist: DomainArtist?): Artist?
    fun mapUrl(url: String, replacement: String): String
}

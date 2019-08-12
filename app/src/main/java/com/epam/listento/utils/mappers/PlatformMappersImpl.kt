package com.epam.listento.utils.mappers

import com.epam.listento.domain.DomainAlbum
import com.epam.listento.domain.DomainArtist
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Album
import com.epam.listento.model.Artist
import com.epam.listento.model.Track
import com.epam.listento.utils.PlatformMappers
import javax.inject.Inject

class PlatformMappersImpl @Inject constructor() : PlatformMappers {
    override fun mapTrack(track: DomainTrack?): Track? {
        return track?.let {
            Track(
                track.id,
                track.durationMs ?: 0,
                track.title ?: "N/A",
                mapArtist(track.artists?.firstOrNull()),
                track.storageDir ?: "",
                mapAlbum(track.albums?.firstOrNull())
            )
        }
    }

    override fun mapAlbum(album: DomainAlbum?): Album? {
        return album?.let {
            val listCover = mapUrl(album.coverUri ?: "", "100x100")
            val albumCover = mapUrl(album.coverUri ?: "", "700x700")
            Album(
                album.id,
                listCover,
                albumCover,
                album.title ?: "N/A",
                mapArtist(album.artists?.firstOrNull())
            )
        }
    }

    override fun mapArtist(artist: DomainArtist?): Artist? {
        return artist?.let {
            val thumbnailUrl = mapUrl(artist.uri ?: "", "100x100")
            val coverUrl = mapUrl(artist.uri ?: "", "700x700")
            Artist(
                artist.id,
                thumbnailUrl,
                coverUrl,
                artist.name ?: "N/A"
            )
        }
    }

    override fun mapUrl(url: String, replacement: String): String {
        return "https://" + url.replace("%%", replacement)
    }
}
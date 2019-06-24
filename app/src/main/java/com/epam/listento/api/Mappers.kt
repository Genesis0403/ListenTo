package com.epam.listento.api

import com.epam.listento.api.model.ApiAlbum
import com.epam.listento.api.model.ApiArtist
import com.epam.listento.api.model.ApiTrack
import com.epam.listento.model.Album
import com.epam.listento.model.Artist
import com.epam.listento.model.Track
import java.util.concurrent.TimeUnit

fun mapTrack(track: ApiTrack?): Track? {
    return track?.let {
        Track(
            track.id,
            durationMapper(track.durationMs ?: 0),
            track.title ?: "N/A",
            mapArtist(track.artists?.firstOrNull()),
            track.storageDir ?: "",
            mapAlbum(track.albums?.firstOrNull())
        )
    }
}

fun mapAlbum(album: ApiAlbum?): Album? {
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

fun mapArtist(artist: ApiArtist?): Artist? {
    return artist?.let {
        val thumbnailUrl = mapUrl(artist.uri ?: "", "100x100")
        val coverUrl = mapUrl(artist.uri ?: "", "700x700")
        return Artist(
            artist.id,
            thumbnailUrl,
            coverUrl,
            artist.name ?: "N/A"
        )
    }
}

private fun mapUrl(url: String, replacement: String): String {
    return "https://" + url.replace("%%", replacement)
}

fun durationMapper(duration: Long): String {
    return String.format(
        "%d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(duration) % TimeUnit.HOURS.toMinutes(1),
        TimeUnit.MILLISECONDS.toSeconds(duration) % TimeUnit.MINUTES.toSeconds(1)
    )
}
package com.epam.listento.api

import com.epam.listento.api.model.ApiAlbum
import com.epam.listento.api.model.ApiArtist
import com.epam.listento.api.model.ApiTrack
import com.epam.listento.model.Album
import com.epam.listento.model.Artist
import com.epam.listento.model.Track
import java.util.concurrent.TimeUnit

fun mapTrack(track: ApiTrack): Track {
    return Track(
        track.id ?: 0,
        durationMapper(track.durationMs ?: 0),
        track.title ?: "N/A",
        mapArtist(track.artists?.firstOrNull()),
        track.storageDir ?: "",
        mapAlbum(track.albums?.firstOrNull())
    )
}

fun mapAlbum(album: ApiAlbum?): Album {
    val listCover = mapUrl(album?.coverUri ?: "", "100x100")
    val albumCover = mapUrl(album?.coverUri ?: "", "700x700")
    return Album(
        album?.id ?: 0,
        listCover,
        albumCover,
        album?.title ?: "N/A",
        mapArtist(album?.artists?.firstOrNull())
    )
}

fun mapArtist(artist: ApiArtist?): Artist {
    val listCover = mapUrl(artist?.uri ?: "", "100x100")
    val albumCover = mapUrl(artist?.uri ?: "", "700x700")
    return Artist(
        artist?.id ?: 0,
        listCover,
        albumCover,
        artist?.name ?: "N/A"
    )
}

private fun mapUrl(url: String, replacement: String): String {
    return "https://" + url.replace("%%", replacement)
}

fun durationMapper(duration: Long): String {
    return String.format("%d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(duration) % TimeUnit.HOURS.toMinutes(1),
        TimeUnit.MILLISECONDS.toSeconds(duration) % TimeUnit.MINUTES.toSeconds(1))
}
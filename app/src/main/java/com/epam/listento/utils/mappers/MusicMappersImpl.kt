package com.epam.listento.utils.mappers

import com.epam.listento.api.model.*
import com.epam.listento.domain.*
import com.epam.listento.utils.MusicMapper
import javax.inject.Inject

class MusicMappersImpl @Inject constructor() : MusicMapper {
    override fun trackToDomain(track: ApiTrack): DomainTrack {
        return DomainTrack(
            track.id,
            track.storageDir,
            track.durationMs,
            track.title,
            track.albums?.map { albumToDomain(it) },
            track.artists?.map { artistToDomain(it) }
        )
    }

    override fun storageToDomain(storage: ApiStorage): DomainStorage {
        return DomainStorage(
            storage.host,
            storage.path,
            storage.ts,
            storage.s
        )
    }

    override fun artistToDomain(artist: ApiArtist): DomainArtist {
        return DomainArtist(
            artist.id,
            artist.uri,
            artist.name,
            artist.cover?.let { coverToDomain(it) }
        )
    }

    override fun albumToDomain(album: ApiAlbum): DomainAlbum {
        return DomainAlbum(
            album.id,
            album.coverUri,
            album.storageDir,
            album.title,
            album.artists?.map { artistToDomain(it) }
        )
    }

    override fun coverToDomain(cover: ApiCover): DomainCover {
        return DomainCover(cover.uri)
    }
}

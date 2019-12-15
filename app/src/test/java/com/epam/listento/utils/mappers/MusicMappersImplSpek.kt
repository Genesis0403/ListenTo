package com.epam.listento.utils.mappers

import com.epam.listento.api.model.ApiAlbum
import com.epam.listento.api.model.ApiArtist
import com.epam.listento.api.model.ApiCover
import com.epam.listento.api.model.ApiTrack
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
object MusicMappersImplSpek : Spek({

    lateinit var mappersImpl: MusicMappersImpl

    fun createMappers() {
        mappersImpl = MusicMappersImpl()
    }

    val albumId = 1
    val albumCover = "someCover"
    val albumStorageDir = "storageDir"
    val albumTitle = "albumTitle"

    val album = ApiAlbum(
        albumId,
        albumCover,
        albumStorageDir,
        albumTitle,
        emptyList()
    )

    val artistId = 1
    val artistUri = "artistCover"
    val artistName = "someArtist"
    val artistCover = ApiCover("someCover")

    val artist = ApiArtist(
        artistId,
        artistUri,
        artistName,
        artistCover
    )

    val trackId = 1
    val trackStorageDir = "storageDir"
    val duration = 0L
    val trackTitle = "someTitle"
    val albums = listOf(album)
    val artists = listOf(artist)

    val track = ApiTrack(
        trackId,
        trackStorageDir,
        duration,
        trackTitle,
        albums,
        artists
    )
    describe("api mapping") {

        beforeEachTest {
            createMappers()
        }

        it("should map track into domain") {
            val dtrack = mappersImpl.trackToDomain(track)
            assertTrue {
                dtrack.id == track.id &&
                        dtrack.title == track.title &&
                        dtrack.durationMs == track.durationMs &&
                        dtrack.storageDir == track.storageDir
            }
        }

        it("should map album into domain") {
            val dalbum = mappersImpl.albumToDomain(album)
            assertTrue {
                dalbum.id == album.id &&
                        dalbum.title == album.title &&
                        dalbum.storageDir == album.storageDir
            }
        }

        it("should map artist into domain") {
            val dartist = mappersImpl.artistToDomain(artist)
            assertTrue {
                dartist.id == artist.id &&
                        dartist.name == artist.name &&
                        dartist.uri == artist.uri
            }
        }
    }
})

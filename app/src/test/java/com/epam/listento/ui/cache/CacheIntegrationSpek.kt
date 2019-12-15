package com.epam.listento.ui.cache

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.epam.listento.R
import com.epam.listento.ServiceHelper
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.DomainAlbum
import com.epam.listento.domain.DomainArtist
import com.epam.listento.domain.DomainCover
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.CustomAlbum
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.repository.MusicRepositoryImpl
import com.epam.listento.repository.global.AlbumsRepository
import com.epam.listento.utils.TestDispatchers
import com.epam.listento.utils.emulateInstanteTaskExecutorRule
import com.epam.listento.utils.mappers.PlatformMappersImpl
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
object CacheIntegrationSpek : Spek({

    emulateInstanteTaskExecutorRule()

    val serviceHelper = mockk<ServiceHelper>(relaxed = true)
    val musicRepo = MusicRepositoryImpl()
    val mappers = PlatformMappersImpl()
    val dispatchers = TestDispatchers()
    val albumsRepo = mockk<AlbumsRepository>(relaxed = true)
    val dao = mockk<TracksDao>(relaxed = true)

    val tracksObserver: Observer<List<Track>> = mockk(relaxed = true)
    val albumsObserver: Observer<List<CustomAlbum>> = mockk(relaxed = true)

    lateinit var viewModel: CacheScreenViewModel

    fun createViewModel() {
        viewModel = CacheScreenViewModel(
            serviceHelper,
            musicRepo,
            mappers,
            dispatchers,
            albumsRepo,
            dao
        )
    }

    val albumId = 1
    val albumCover = "someCover"
    val albumStorageDir = "storageDir"
    val albumTitle = "albumTitle"

    val mockedDomainAlbum = DomainAlbum(
        albumId,
        albumCover,
        albumStorageDir,
        albumTitle,
        emptyList()
    )

    val artistId = 1
    val artistUri = "artistCover"
    val artistName = "someArtist"
    val artistDomainCover = "someCover"
    val artistCover = DomainCover(artistDomainCover)

    val mockedDomainArtist = DomainArtist(
        artistId,
        artistUri,
        artistName,
        artistCover
    )

    val trackId = 1
    val trackStorageDir = "storageDir"
    val duration = 0L
    val trackTitle = "someTitle"
    val albums = listOf(mockedDomainAlbum)
    val artists = listOf(mockedDomainArtist)

    val mockedDomainTrack = DomainTrack(
        trackId,
        trackStorageDir,
        duration,
        trackTitle,
        albums,
        artists
    )

    val tracks = listOf(mockedDomainTrack)

    val platformTrack = mappers.mapTrack(mockedDomainTrack)!!

    val customAlbumTitle = "title"
    val customAlbumArtist = "artist"
    val customAlbumCover = "cover"
    val customAlbumId = 1

    val customAlbum = CustomAlbum(
        customAlbumTitle,
        customAlbumArtist,
        customAlbumCover,
        listOf(platformTrack),
        customAlbumId
    )

    describe("tracks fetching from cache") {

        beforeEachTest {
            every { serviceHelper.currentPlaying.value } returns trackId
            every { serviceHelper.playbackState.value } returns PlaybackState.Stopped
            every { dao.getLiveDataTracks() } returns MutableLiveData(tracks)
            every { albumsRepo.getAlbums() } returns MutableLiveData(listOf(customAlbum))
            createViewModel()
            viewModel.tracks.observeForever(tracksObserver)
            viewModel.albums.observeForever(albumsObserver)
        }

        afterEachTest {
            viewModel.tracks.removeObserver(tracksObserver)
            clearMocks(serviceHelper, tracksObserver, albumsObserver, dao, albumsRepo)
        }

        it("should fetch") {
            val track = viewModel.tracks.value?.firstOrNull()
            val album = viewModel.albums.value?.firstOrNull()
            assertTrue {
                track?.id == mockedDomainTrack.id &&
                        album?.id == customAlbumId
            }
        }

    }

    describe("player state changes") {

        beforeEachTest {
            every { serviceHelper.currentPlaying.value } returns -1
            every { dao.getLiveDataTracks() } returns MutableLiveData(tracks)
            createViewModel()
            viewModel.tracks.observeForever(tracksObserver)
        }

        afterEachTest {
            viewModel.tracks.removeObserver(tracksObserver)
            clearMocks(serviceHelper, tracksObserver, dao)
        }

        it("should change track's play resource to playing") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Paused
            viewModel.handlePlayerStateChange(trackId)

            val value = viewModel.tracks.value
            val track = value?.first()
            assertTrue {
                track?.id == trackId &&
                        track.res == R.drawable.exo_icon_play
            }
        }

        it("should change track's resource to paused") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            viewModel.handlePlayerStateChange(trackId)

            val value = viewModel.tracks.value
            val track = value?.first()
            assertTrue {
                track?.id == trackId &&
                        track.res == R.drawable.exo_icon_pause
            }
        }

        it("should change track's resource to nothing") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Stopped
            viewModel.handlePlayerStateChange(trackId)

            val value = viewModel.tracks.value
            val track = value?.first()
            assertTrue {
                track?.id == trackId &&
                        track.res == Track.NO_RES
            }
        }
    }
})

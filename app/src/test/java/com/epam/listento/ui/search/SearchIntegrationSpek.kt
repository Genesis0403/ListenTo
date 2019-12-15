package com.epam.listento.ui.search

import androidx.lifecycle.Observer
import com.epam.listento.R
import com.epam.listento.ServiceHelper
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.Status
import com.epam.listento.domain.DomainAlbum
import com.epam.listento.domain.DomainArtist
import com.epam.listento.domain.DomainCover
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.player.utils.id
import com.epam.listento.repository.MusicRepositoryImpl
import com.epam.listento.repository.global.TracksRepository
import com.epam.listento.utils.TestDispatchers
import com.epam.listento.utils.emulateInstanteTaskExecutorRule
import com.epam.listento.utils.mappers.PlatformMappersImpl
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
object SearchIntegrationSpek : Spek({

    emulateInstanteTaskExecutorRule()

    val serviceHelper = mockk<ServiceHelper>(relaxed = true)
    val tracksRepo = mockk<TracksRepository>(relaxed = true)
    val musicRepo = MusicRepositoryImpl()
    val mappers = PlatformMappersImpl()
    val dispatchers = TestDispatchers()

    val tracksObserver: Observer<ApiResponse<List<Track>>> = mockk(relaxed = true)

    lateinit var viewModel: SearchScreenViewModel

    fun createViewModel() {
        viewModel = SearchScreenViewModel(
            serviceHelper,
            tracksRepo,
            musicRepo,
            mappers,
            dispatchers
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

    describe("tracks fetching") {

        val mockedQuery = "some query"
        val mockedError = "this is a error"

        beforeEachTest {
            createViewModel()
            viewModel.tracks.observeForever(tracksObserver)
        }

        afterEachTest {
            viewModel.tracks.removeObserver(tracksObserver)
            clearMocks(tracksObserver, tracksRepo)
        }

        it("should fetch successfully") {
            coEvery { tracksRepo.fetchTracks(mockedQuery) } returns ApiResponse.success(tracks)
            viewModel.fetchTracks(mockedQuery)

            val value = viewModel.tracks.value
            val tracks = value?.body
            val track = tracks?.firstOrNull()
            val album = track?.album
            val artist = track?.artist
            assertTrue {
                value?.status == Status.SUCCESS &&
                        tracks?.isNotEmpty() ?: false &&
                        track != null &&
                        track.id == trackId &&
                        track.title == trackTitle &&
                        track.duration == duration &&
                        album != null &&
                        album.id == albumId &&
                        album.title == albumTitle &&
                        artist != null &&
                        artist.id == artistId &&
                        artist.name == artistName
            }
        }

        it("should fail") {
            coEvery { tracksRepo.fetchTracks(mockedQuery) } returns ApiResponse.error(mockedError)
            viewModel.fetchTracks(mockedQuery)

            val value = viewModel.tracks.value
            assertTrue {
                value?.status == Status.ERROR &&
                        value.body == null &&
                        value.error == mockedError
            }
        }
    }

    describe("player state changes") {

        val mockedQuery = "some query"

        beforeEachTest {
            coEvery { tracksRepo.fetchTracks(mockedQuery) } returns ApiResponse.success(tracks)
            createViewModel()
            viewModel.tracks.observeForever(tracksObserver)
            viewModel.fetchTracks(mockedQuery)
        }

        afterEachTest {
            viewModel.tracks.removeObserver(tracksObserver)
            clearMocks(tracksRepo, serviceHelper, tracksObserver)
        }

        it("should change track's play resource to playing") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Paused
            viewModel.handlePlayerStateChange(trackId)

            val value = viewModel.tracks.value
            val track = value?.body?.first()
            assertTrue {
                value?.status == Status.SUCCESS &&
                        track?.id == trackId &&
                        track.res == R.drawable.exo_icon_play
            }
        }

        it("should change track's resource to paused") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            viewModel.handlePlayerStateChange(trackId)

            val value = viewModel.tracks.value
            val track = value?.body?.first()
            assertTrue {
                value?.status == Status.SUCCESS &&
                        track?.id == trackId &&
                        track.res == R.drawable.exo_icon_pause
            }
        }

        it("should change track's resource to nothing") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Stopped
            viewModel.handlePlayerStateChange(trackId)

            val value = viewModel.tracks.value
            val track = value?.body?.first()
            assertTrue {
                value?.status == Status.SUCCESS &&
                        track?.id == trackId &&
                        track.res == Track.NO_RES
            }
        }
    }

    describe("item click") {

        val mockedQuery = "some query"

        beforeEachTest {
            coEvery { tracksRepo.fetchTracks(mockedQuery) } returns ApiResponse.success(tracks)

            createViewModel()
            viewModel.tracks.observeForever(tracksObserver)
        }

        afterEachTest {
            clearMocks(tracksRepo)
        }

        it("should change current playing") {
            val track = viewModel.tracks.value?.body?.first() ?: return@it

            viewModel.handleItemClick(track)

            assertTrue {
                musicRepo.getCurrent().id == track.id.toString()
            }
        }
    }
})

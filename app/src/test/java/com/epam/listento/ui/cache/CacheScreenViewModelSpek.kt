package com.epam.listento.ui.cache

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.epam.listento.R
import com.epam.listento.ServiceHelper
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Album
import com.epam.listento.model.Artist
import com.epam.listento.model.CustomAlbum
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.repository.global.AlbumsRepository
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.PlatformMappers
import com.epam.listento.utils.TestDispatchers
import com.epam.listento.utils.emulateInstanteTaskExecutorRule
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
object CacheScreenViewModelSpek : Spek({

    emulateInstanteTaskExecutorRule()

    val musicRepo: MusicRepository = mockk(relaxed = true)
    val albumsRepo: AlbumsRepository = mockk(relaxed = true)
    val mappers: PlatformMappers = mockk(relaxed = true)
    val dao: TracksDao = mockk(relaxed = true)
    val dispatchers: AppDispatchers = TestDispatchers()
    val serviceHelper: ServiceHelper = mockk(relaxed = true)

    var commandObserver: Observer<CacheScreenViewModel.Command> = mockk(relaxed = true)
    val tracksObserver: Observer<List<Track>> = mockk(relaxed = true)

    val currentTrack: Track = mockk(relaxed = true)
    val artist: Artist = mockk(relaxed = true)
    val album: Album = mockk(relaxed = true)

    lateinit var viewModel: CacheScreenViewModel

    val mockedId = 1
    val mockedDuration = 0L
    val mockedTitle = ""
    val mockedStorage = ""
    val mockedResId = 0

    val mockedTrack = Track(
        mockedId,
        mockedDuration,
        mockedTitle,
        artist,
        mockedStorage,
        album,
        mockedResId
    )

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

    describe("track item click") {

        val fakeId = 2
        val track = Track(
            fakeId,
            mockedDuration,
            mockedTitle,
            artist,
            mockedStorage,
            album,
            mockedResId
        )

        beforeEachTest {
            createViewModel()
            commandObserver = mockk(relaxed = true)
            viewModel.command.observeForever(commandObserver)
            every { currentTrack.id } returns mockedId
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            every { serviceHelper.currentPlaying.value } returns currentTrack.id
        }

        afterEachTest {
            viewModel.command.removeObserver(commandObserver)
            clearMocks(commandObserver, serviceHelper)
        }

        it("should open player activity") {
            viewModel.handleTrackClick(mockedTrack)
            verify { commandObserver.onChanged(CacheScreenViewModel.Command.ShowPlayerActivity) }
        }

        it("should change playlist") {
            viewModel.handleTrackClick(track)
            verify { commandObserver.onChanged(CacheScreenViewModel.Command.PlayTrack) }
        }

        it("should play track when ids are not equals") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            viewModel.handleTrackClick(track)
            verify { commandObserver.onChanged(CacheScreenViewModel.Command.PlayTrack) }
        }

        it("should play track when state is stopped") {
            every { currentTrack.id } returns fakeId
            every { serviceHelper.playbackState.value } returns PlaybackState.Stopped
            viewModel.handleTrackClick(track)
            verify { commandObserver.onChanged(CacheScreenViewModel.Command.PlayTrack) }
        }

        it("should play track when state is paused") {
            every { currentTrack.id } returns fakeId
            every { serviceHelper.playbackState.value } returns PlaybackState.Paused
            viewModel.handleTrackClick(track)
            verify { commandObserver.onChanged(CacheScreenViewModel.Command.PlayTrack) }
        }
    }

    describe("long item click") {

        beforeEachTest {
            createViewModel()
            commandObserver = mockk(relaxed = true)
            viewModel.command.observeForever(commandObserver)
            every { artist.name } returns ""
        }

        afterEachTest {
            viewModel.command.removeObserver(commandObserver)
            clearMocks(commandObserver)
        }

        it("should show cache dialog") {
            viewModel.handleThreeDotButtonClick(mockedTrack)
            assertTrue { viewModel.command.value is CacheScreenViewModel.Command.ShowCacheDialog }
        }
    }

    describe("album item click") {

        val mockedAlbum: CustomAlbum = mockk(relaxed = true)

        beforeEachTest {
            createViewModel()
            commandObserver = mockk(relaxed = true)
            viewModel.command.observeForever(commandObserver)
            every { artist.name } returns ""
        }

        afterEachTest {
            viewModel.command.removeObserver(commandObserver)
            clearMocks(commandObserver, mockedAlbum)
        }

        it("should open album") {
            viewModel.handleAlbumClick(mockedAlbum)
            val value = viewModel.command.value
            assertTrue {
                value is CacheScreenViewModel.Command.ShowAlbumActivity &&
                        value.cover == mockedAlbum.cover &&
                        value.id == mockedAlbum.id &&
                        value.title == mockedAlbum.title
            }
        }
    }

    describe("3dot menu click") {

        beforeEachTest {
            createViewModel()
            commandObserver = mockk(relaxed = true)
            viewModel.command.observeForever(commandObserver)
            every { artist.name } returns ""
        }

        afterEachTest {
            viewModel.command.removeObserver(commandObserver)
            clearMocks(commandObserver)
        }

        it("should show cache dialog") {
            viewModel.handleThreeDotButtonClick(mockedTrack)
            val value = viewModel.command.value
            assertTrue {
                value is CacheScreenViewModel.Command.ShowCacheDialog &&
                        value.id == mockedTrack.id &&
                        value.title == mockedTrack.title &&
                        value.artist == mockedTrack.artist?.name
            }
        }
    }

    describe("player state change") {

        val domainTrack = mockk<DomainTrack>(relaxed = true)
        val mockedDomainTracks = listOf(domainTrack)

        beforeEachTest {
            every { mappers.mapTrack(domainTrack) } returns mockedTrack
            every { serviceHelper.currentPlaying.value } returns -1
            every { dao.getLiveDataTracks() } returns MutableLiveData(mockedDomainTracks)
            createViewModel()
            viewModel.tracks.observeForever(tracksObserver)
        }

        afterEachTest {
            clearMocks(tracksObserver, serviceHelper, domainTrack, mappers, dao)
        }

        it("should post tracks where track has pause icon") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            viewModel.handlePlayerStateChange(mockedId)

            assertTrue {
                viewModel.tracks.value?.lastOrNull()?.res == R.drawable.exo_icon_pause
            }
        }

        it("should post tracks where track has play icon") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Paused
            viewModel.handlePlayerStateChange(mockedId)

            assertTrue {
                viewModel.tracks.value?.lastOrNull()?.res == R.drawable.exo_icon_play
            }
        }

        it("should post tracks where track doesn't have icon") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Paused
            viewModel.handlePlayerStateChange(-1)

            assertTrue {
                viewModel.tracks.value?.lastOrNull()?.res == Track.NO_RES
            }
        }
    }
})

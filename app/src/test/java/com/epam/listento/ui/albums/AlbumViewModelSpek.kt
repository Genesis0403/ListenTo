package com.epam.listento.ui.albums

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.epam.listento.R
import com.epam.listento.ServiceHelper
import com.epam.listento.model.Artist
import com.epam.listento.model.CustomAlbum
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.toMetadata
import com.epam.listento.repository.global.AlbumsRepository
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.TestDispatchers
import com.epam.listento.utils.emulateInstanteTaskExecutorRule
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
object AlbumViewModelSpek : Spek({

    emulateInstanteTaskExecutorRule()

    val serviceHelper = mockk<ServiceHelper>(relaxed = true)
    val dispatchers = TestDispatchers()
    val musicRepo = mockk<MusicRepository>(relaxed = true, relaxUnitFun = true)
    val title = "someTitle"
    val albumsRepo = mockk<AlbumsRepository>(relaxed = true)
    val id = 999

    val tracksObserver: Observer<List<Track>> = mockk(relaxed = true)
    val commandObserver: Observer<AlbumViewModel.Command> = mockk(relaxed = true)

    lateinit var viewModel: AlbumViewModel

    fun createViewModel() {
        viewModel = AlbumViewModel(
            serviceHelper,
            dispatchers,
            musicRepo,
            title,
            albumsRepo,
            id
        )
    }

    describe("track click") {

        val mockedTrack = mockk<Track>(relaxed = true)
        val mockedId = 0
        val fakeId = -1

        beforeEachTest {
            createViewModel()
            every { musicRepo.setCurrent(any()) } just Runs
            viewModel.command.observeForever(commandObserver)
            every { mockedTrack.id } returns mockedId
        }

        afterEachTest {
            clearMocks(commandObserver, serviceHelper, musicRepo, albumsRepo)
        }

        it("should play track when id isn't equals") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            every { serviceHelper.currentPlaying.value } returns fakeId
            viewModel.handleClick(mockedTrack)
            verify { commandObserver.onChanged(AlbumViewModel.Command.PlayTrack) }
        }

        it("should play track when state is not playing") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Stopped
            every { serviceHelper.currentPlaying.value } returns mockedId
            viewModel.handleClick(mockedTrack)
            verify { commandObserver.onChanged(AlbumViewModel.Command.PlayTrack) }
        }

        it("should pause track when ids are equal and state is playing") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            every { serviceHelper.currentPlaying.value } returns mockedId
            viewModel.handleClick(mockedTrack)
            verify { commandObserver.onChanged(AlbumViewModel.Command.PauseTrack) }
        }
    }

    describe("player state change") {

        val mockedAlbum = mockk<CustomAlbum>(relaxed = true)

        val mockedId = 1
        val mockedDuration = 0L
        val mockedTitle = ""
        val mockedStorage = ""
        val mockedResId = -1

        val mockedTrack = Track(
            mockedId,
            mockedDuration,
            mockedTitle,
            mockk(relaxed = true),
            mockedStorage,
            mockk(relaxed = true),
            mockedResId
        )

        beforeEachTest {
            every { musicRepo.setSource(any()) } just Runs
            every { mockedAlbum.tracks } returns listOf(mockedTrack)
            every { albumsRepo.getAlbumById(id) } returns MutableLiveData(mockedAlbum)
            createViewModel()
            viewModel.tracks.observeForever(tracksObserver)
        }

        afterEachTest {
            clearMocks(tracksObserver, serviceHelper, albumsRepo)
        }

        it("should post tracks where track has pause icon") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            viewModel.handlePlayerStateChange(mockedId)

            assertTrue {
                viewModel.tracks.value?.lastOrNull()?.res == R.drawable.lt_pause_icon
            }
        }

        it("should post tracks where track has play icon") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Paused
            viewModel.handlePlayerStateChange(mockedId)

            assertTrue {
                viewModel.tracks.value?.lastOrNull()?.res == R.drawable.lt_play_icon
            }
        }

        it("should post tracks where track doesn't have icon") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Stopped
            viewModel.handlePlayerStateChange(-1)

            assertTrue {
                viewModel.tracks.value?.lastOrNull()?.res == Track.NO_RES
            }
        }
    }
})

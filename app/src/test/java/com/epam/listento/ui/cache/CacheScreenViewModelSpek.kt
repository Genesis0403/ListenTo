package com.epam.listento.ui.cache

import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.Observer
import com.epam.listento.db.TracksDao
import com.epam.listento.model.Album
import com.epam.listento.model.Artist
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.repository.global.AlbumsRepository
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.PlatformMappers
import com.epam.listento.utils.emulateInstanteTaskExecutorRule
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
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
    val dispatchers: AppDispatchers = mockk(relaxed = true)

    var stateObserver: Observer<PlaybackState> = mockk(relaxed = true)
    var commandObserver: Observer<CacheScreenViewModel.Command> = mockk(relaxed = true)
    var currentObserver: Observer<Track> = mockk(relaxed = true)

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
        viewModel = spyk(
            CacheScreenViewModel(
                musicRepo,
                mappers,
                dispatchers,
                albumsRepo,
                dao
            )
        )
    }

    beforeEachTest {
        every { dispatchers.ui } returns Dispatchers.Unconfined
        every { dispatchers.default } returns Dispatchers.Unconfined
        every { dispatchers.io } returns Dispatchers.Unconfined
    }

    afterEachTest {
        clearMocks(dispatchers)
    }

    describe("item click") {

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
            every { viewModel.playbackState.value } returns PlaybackState.Playing
            every { viewModel.currentPlaying.value } returns currentTrack
            every { currentTrack.id } returns mockedId
        }

        afterEachTest {
            viewModel.command.removeObserver(commandObserver)
            clearMocks(commandObserver)
        }

        it("should navigate to player") {
            viewModel.handleTrackClick(mockedTrack)
            verify { commandObserver.onChanged(CacheScreenViewModel.Command.ShowPlayerActivity) }
        }

        it("should change playlist") {
            viewModel.handleTrackClick(track)
            verify { commandObserver.onChanged(CacheScreenViewModel.Command.PlayTrack) }
        }

        it("should play track when ids are not equals") {
            every { viewModel.playbackState.value } returns PlaybackState.Playing
            viewModel.handleTrackClick(track)
            verify { commandObserver.onChanged(CacheScreenViewModel.Command.PlayTrack) }
        }

        it("should play track when state is stopped") {
            every { currentTrack.id } returns fakeId
            every { viewModel.playbackState.value } returns PlaybackState.Stopped
            viewModel.handleTrackClick(track)
            verify { commandObserver.onChanged(CacheScreenViewModel.Command.PlayTrack) }
        }

        it("should play track when state is paused") {
            every { currentTrack.id } returns fakeId
            every { viewModel.playbackState.value } returns PlaybackState.Paused
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

    describe("metadata changes") {

        beforeEachTest {
            currentObserver = mockk(relaxed = true)
            createViewModel()
            viewModel.currentPlaying.observeForever(currentObserver)
            every { viewModel.tracks.value } returns listOf(mockedTrack)
        }

        afterEachTest {
            viewModel.currentPlaying.removeObserver(currentObserver)
            clearMocks(musicRepo, currentObserver)
        }

        it("should change current playing") {
            viewModel.handleMetadataChange(mockedId)
            verify { currentObserver.onChanged(mockedTrack) }
        }

        it("should not change") {
            val fakeId = -1
            viewModel.handleMetadataChange(fakeId)
            verify(inverse = true) { currentObserver.onChanged(mockedTrack) }
        }
    }

    describe("playback state changes") {

        beforeEachTest {
            createViewModel()
            stateObserver = mockk(relaxed = true)
            viewModel.playbackState.observeForever(stateObserver)
        }

        afterEachTest {
            viewModel.playbackState.removeObserver(stateObserver)
            clearMocks(stateObserver)
        }

        it("should be playing") {
            viewModel.handlePlaybackStateChange(PlaybackStateCompat.STATE_PLAYING)
            verify { stateObserver.onChanged(PlaybackState.Playing) }
        }

        it("should be paused") {
            viewModel.handlePlaybackStateChange(PlaybackStateCompat.STATE_PAUSED)
            verify { stateObserver.onChanged(PlaybackState.Paused) }
        }

        it("should be stopped") {
            viewModel.handlePlaybackStateChange(PlaybackStateCompat.STATE_STOPPED)
            verify { stateObserver.onChanged(PlaybackState.Stopped) }
        }
    }
})

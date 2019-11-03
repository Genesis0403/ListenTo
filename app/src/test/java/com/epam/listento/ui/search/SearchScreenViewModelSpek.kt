package com.epam.listento.ui.search

import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.Observer
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.Status
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Album
import com.epam.listento.model.Artist
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.repository.global.TracksRepository
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.emulateInstanteTaskExecutorRule
import io.mockk.clearMocks
import io.mockk.coEvery
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
object SearchScreenViewModelSpek : Spek({

    emulateInstanteTaskExecutorRule()

    val musicRepo: MusicRepository = mockk(relaxed = true)
    val tracksRepo: TracksRepository = mockk(relaxed = true)
    val dispatchers: AppDispatchers = mockk(relaxed = true)

    var commandObserver: Observer<SearchScreenViewModel.Command> = mockk(relaxed = true)
    var currentObserver: Observer<Track> = mockk(relaxed = true)
    var stateObserver: Observer<PlaybackState> = mockk(relaxed = true)
    var tracksObserver: Observer<ApiResponse<List<Track>>> = mockk(relaxed = true)

    val currentTrack: Track = mockk(relaxed = true)
    val artist: Artist? = mockk(relaxed = true)
    val album: Album = mockk(relaxed = true)

    lateinit var viewModel: SearchScreenViewModel

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
            SearchScreenViewModel(
                tracksRepo,
                musicRepo,
                dispatchers
            )
        )
    }

    beforeEachTest {
        every { dispatchers.default } returns Dispatchers.Unconfined
        every { dispatchers.ui } returns Dispatchers.Unconfined
        every { dispatchers.io } returns Dispatchers.Unconfined
    }

    afterEachTest {
        clearMocks(dispatchers)
    }

    describe("tracks fetching") {

        val mockedQury30 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val mockedQuery = "some_track"
        val mockedError = "some_error"
        var mockedFetchResponse: ApiResponse<List<DomainTrack>> = mockk(relaxed = true)

        beforeEachTest {
            createViewModel()
            tracksObserver = mockk(relaxed = true)
            mockedFetchResponse = mockk(relaxed = true)
            viewModel.tracks.observeForever(tracksObserver)
            viewModel.command.observeForever(commandObserver)

            coEvery { tracksRepo.fetchTracks(mockedQuery) } returns mockedFetchResponse
        }

        afterEachTest {
            viewModel.tracks.removeObserver(tracksObserver)
            viewModel.command.removeObserver(commandObserver)
            clearMocks(tracksObserver, commandObserver, tracksRepo, mockedFetchResponse)
        }

        it("should prevent loading when query is empty") {
            viewModel.fetchTracks("")
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.StopLoading) }
        }

        it("should prevent loading when query's length more then 30") {
            viewModel.fetchTracks(mockedQury30)
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.StopLoading) }
        }

        it("should fetch successfully") {
            every { mockedFetchResponse.status } returns Status.SUCCESS
            every { mockedFetchResponse.body } returns emptyList()

            viewModel.fetchTracks(mockedQuery)
            verify { tracksObserver.onChanged(any()) }
            assertTrue { viewModel.tracks.value?.status == Status.SUCCESS }
        }

        it("should be an error") {
            every { mockedFetchResponse.status } returns Status.ERROR
            every { mockedFetchResponse.error } returns mockedError

            viewModel.fetchTracks(mockedQuery)
            verify { tracksObserver.onChanged(any()) }
            assertTrue { viewModel.tracks.value?.status == Status.ERROR }
        }
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
            every { viewModel.currentPlaying.value } returns currentTrack
            every { currentTrack.id } returns mockedId
        }

        afterEachTest {
            viewModel.command.removeObserver(commandObserver)
            clearMocks(commandObserver)
        }

        it("should navigate to player when state is plying and id equals") {
            viewModel.handleItemClick(mockedTrack)
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.ShowPlayerActivity) }
        }

        it("should play track when ids are not equals") {
            every { viewModel.playbackState.value } returns PlaybackState.Playing
            viewModel.handleItemClick(track)
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.PlayTrack) }
        }

        it("should play track when state is stopped") {
            every { currentTrack.id } returns fakeId
            every { viewModel.playbackState.value } returns PlaybackState.Stopped
            viewModel.handleItemClick(track)
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.PlayTrack) }
        }

        it("should play track when state is paused") {
            every { currentTrack.id } returns fakeId
            every { viewModel.playbackState.value } returns PlaybackState.Paused
            viewModel.handleItemClick(track)
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.PlayTrack) }
        }
    }

    describe("long item click") {

        beforeEachTest {
            createViewModel()
            commandObserver = mockk(relaxed = true)
            viewModel.command.observeForever(commandObserver)
        }

        afterEachTest {
            viewModel.command.removeObserver(commandObserver)
            clearMocks(commandObserver)
        }

        it("should show cache dialog when artist's name is not null") {
            every { artist?.name } returns ""
            viewModel.handleLongItemClick(mockedTrack)
            assertTrue { viewModel.command.value is SearchScreenViewModel.Command.ShowCacheDialog }
        }

        it("should show cache dialog when artist's name is null") {
            every { artist?.name } returns null
            viewModel.handleLongItemClick(mockedTrack)
            assertTrue { viewModel.command.value is SearchScreenViewModel.Command.ShowCacheDialog }
        }
    }

    describe("metadata changes") {

        beforeEachTest {
            currentObserver = mockk(relaxed = true)
            createViewModel()
            viewModel.currentPlaying.observeForever(currentObserver)
            every { viewModel.tracks.value?.body } returns listOf(mockedTrack)
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

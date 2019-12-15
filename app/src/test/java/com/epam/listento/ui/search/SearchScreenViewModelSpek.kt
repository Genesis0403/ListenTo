package com.epam.listento.ui.search

import androidx.lifecycle.Observer
import com.epam.listento.R
import com.epam.listento.ServiceHelper
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
import com.epam.listento.utils.PlatformMappers
import com.epam.listento.utils.TestDispatchers
import com.epam.listento.utils.emulateInstanteTaskExecutorRule
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
object SearchScreenViewModelSpek : Spek({

    emulateInstanteTaskExecutorRule()

    val musicRepo: MusicRepository = mockk(relaxed = true)
    val tracksRepo: TracksRepository = mockk(relaxed = true)
    val dispatchers: AppDispatchers = TestDispatchers()
    val serviceHelper: ServiceHelper = mockk(relaxed = true)
    val mappers: PlatformMappers = mockk(relaxed = true)

    var commandObserver: Observer<SearchScreenViewModel.Command> = mockk(relaxed = true)
    var currentObserver: Observer<Int> = mockk(relaxed = true)
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
        viewModel =
            SearchScreenViewModel(
                serviceHelper,
                tracksRepo,
                musicRepo,
                mappers,
                dispatchers
            )
    }

    describe("tracks fetching") {

        val mockedQuery30 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val mockedQuery31 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        val mockedQuery32 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
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

        it("should prevent loading when query's length is 31") {
            viewModel.fetchTracks(mockedQuery31)
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.StopLoading) }
        }

        it("should prevent loading when query's length more then 30") {
            viewModel.fetchTracks(mockedQuery32)
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.StopLoading) }
        }

        it("should fetch successfully") {
            every { mockedFetchResponse.status } returns Status.SUCCESS
            every { mockedFetchResponse.body } returns emptyList()
            coEvery { tracksRepo.fetchTracks(mockedQuery) } returns mockedFetchResponse

            viewModel.fetchTracks(mockedQuery)
            verify { tracksObserver.onChanged(any()) }
            assertTrue { viewModel.tracks.value?.status == Status.SUCCESS }
        }

        it("should fetch successfully with query length 30") {
            every { mockedFetchResponse.status } returns Status.SUCCESS
            every { mockedFetchResponse.body } returns emptyList()
            coEvery { tracksRepo.fetchTracks(mockedQuery30) } returns mockedFetchResponse

            viewModel.fetchTracks(mockedQuery30)
            assertTrue { viewModel.tracks.value?.status == Status.SUCCESS }
            assertTrue { viewModel.tracks.value?.body.isNullOrEmpty() }
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
            every { currentTrack.id } returns mockedId
            every { serviceHelper.currentPlaying.value } returns mockedId
        }

        afterEachTest {
            viewModel.command.removeObserver(commandObserver)
            clearMocks(commandObserver, serviceHelper)
        }

        it("should navigate to player when state is plying and id equals") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            viewModel.handleItemClick(mockedTrack)
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.ShowPlayerActivity) }
        }

        it("should play track when ids are not equals") {
            every { currentTrack.id } returns -1
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            viewModel.handleItemClick(track)
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.PlayTrack) }
        }

        it("should play track when state is stopped") {
            every { currentTrack.id } returns fakeId
            every { serviceHelper.playbackState.value } returns PlaybackState.Stopped
            viewModel.handleItemClick(track)
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.PlayTrack) }
        }

        it("should play track when state is paused") {
            every { currentTrack.id } returns fakeId
            every { serviceHelper.playbackState.value } returns PlaybackState.Paused
            viewModel.handleItemClick(track)
            verify { commandObserver.onChanged(SearchScreenViewModel.Command.PlayTrack) }
        }
    }

    describe("3dot menu click") {

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
            viewModel.handleTheeDotMenuClick(mockedTrack)
            assertTrue { viewModel.command.value is SearchScreenViewModel.Command.ShowCacheDialog }
        }

        it("should show cache dialog when artist's name is null") {
            every { artist?.name } returns null
            viewModel.handleTheeDotMenuClick(mockedTrack)
            assertTrue { viewModel.command.value is SearchScreenViewModel.Command.ShowCacheDialog }
        }
    }

    describe("metadata changes") {

        beforeEachTest {
            currentObserver = mockk(relaxed = true)
            createViewModel()
            serviceHelper.currentPlaying.observeForever(currentObserver)
            every { viewModel.tracks.value?.body } returns listOf(mockedTrack)
        }

        afterEachTest {
            serviceHelper.currentPlaying.removeObserver(currentObserver)
            clearMocks(musicRepo, currentObserver)
        }
    }

    describe("playback state changes") {

        beforeEachTest {
            createViewModel()
            stateObserver = mockk(relaxed = true)
            serviceHelper.playbackState.observeForever(stateObserver)
        }

        afterEachTest {
            serviceHelper.playbackState.removeObserver(stateObserver)
            clearMocks(stateObserver)
        }
    }

    describe("player state change") {

        val mockedQuery = "gvgvggg"
        val domainTrack = mockk<DomainTrack>(relaxed = true)
        val mockedDomainTracks = listOf(domainTrack)

        beforeEachTest {
            coEvery {
                tracksRepo.fetchTracks(mockedQuery)
            } returns ApiResponse.success(mockedDomainTracks)
            every { mappers.mapTrack(domainTrack) } returns mockedTrack
            every { serviceHelper.currentPlaying.value } returns -1
            createViewModel()
            viewModel.tracks.observeForever(tracksObserver)
            viewModel.fetchTracks(mockedQuery)
        }

        afterEachTest {
            clearMocks(tracksObserver, serviceHelper, domainTrack, tracksRepo)
        }

        it("should post tracks where track has pause icon") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            viewModel.handlePlayerStateChange(mockedId)

            assertTrue {
                viewModel.tracks.value?.body?.lastOrNull()?.res == R.drawable.exo_icon_pause
            }
        }

        it("should post tracks where track has play icon") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Paused
            viewModel.handlePlayerStateChange(mockedId)

            assertTrue {
                viewModel.tracks.value?.body?.lastOrNull()?.res == R.drawable.exo_icon_play
            }
        }

        it("should post tracks where track doesn't have icon") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Paused
            viewModel.handlePlayerStateChange(-1)

            assertTrue {
                viewModel.tracks.value?.body?.lastOrNull()?.res == Track.NO_RES
            }
        }
    }
})

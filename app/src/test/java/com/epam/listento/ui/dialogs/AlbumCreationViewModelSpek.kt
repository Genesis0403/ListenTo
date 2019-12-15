package com.epam.listento.ui.dialogs

import androidx.lifecycle.Observer
import com.epam.listento.R
import com.epam.listento.model.Track
import com.epam.listento.repository.global.AlbumsRepository
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.ui.albums.AlbumCreationViewModel
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.ContextProvider
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
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
object AlbumCreationViewModelSpek : Spek({

    emulateInstanteTaskExecutorRule()

    val albumsRepo: AlbumsRepository = mockk(relaxed = true)
    val trackRepo: TrackRepository = mockk(relaxed = true)
    val dispatchers: AppDispatchers = mockk(relaxed = true)
    val contextProvider: ContextProvider = mockk(relaxed = true)

    val commandObserver: Observer<AlbumCreationViewModel.Command> = mockk(relaxed = true)

    lateinit var viewModel: AlbumCreationViewModel

    fun createViewModel() {
        viewModel = spyk(
            AlbumCreationViewModel(
                albumsRepo,
                trackRepo,
                dispatchers,
                contextProvider
            )
        )
    }

    beforeEachTest {
        createViewModel()
    }

    afterEachTest {
        every { dispatchers.ui } returns Dispatchers.Unconfined
        every { dispatchers.default } returns Dispatchers.Unconfined
        every { dispatchers.io } returns Dispatchers.Unconfined
    }

    describe("menu item clicks") {

        beforeEachTest {
            viewModel.command.observeForever(commandObserver)
        }

        afterEachTest {
            clearMocks(commandObserver)
        }

        it("should save item be clicked") {
            viewModel.onMenuItemClick(R.id.saveItem)
            verify { commandObserver.onChanged(AlbumCreationViewModel.Command.SaveAlbum) }
        }

        it("should add image item be clicked") {
            viewModel.onMenuItemClick(R.id.addImage)
            verify { commandObserver.onChanged(AlbumCreationViewModel.Command.ChangeCover) }
        }

        it("should go back when back arrow clicked") {
            viewModel.onMenuItemClick(android.R.id.home)
            verify { commandObserver.onChanged(AlbumCreationViewModel.Command.CloseActivity) }
        }

        it("should not match any of the actions") {
            val wrongId = -1
            assertFalse { viewModel.onMenuItemClick(wrongId) }
        }
    }

    describe("track click") {

        val mockedTrack: Track = mockk(relaxed = true)

        beforeEachTest {
            viewModel.command.observeForever(commandObserver)
        }

        afterEachTest {
            clearMocks(commandObserver)
        }

        it("track should be added") {
            viewModel.onTrackClick(mockedTrack)
            assertTrue { viewModel.checkedTracks.contains(mockedTrack) }
        }

        it("track should be removed") {
            viewModel.onTrackClick(mockedTrack)
            viewModel.onTrackClick(mockedTrack)
            assertFalse { viewModel.checkedTracks.contains(mockedTrack) }
        }
    }

    describe("cover change") {

        val mockedUrl = "mockedUrl"

        beforeEachTest {
            viewModel.command.observeForever(commandObserver)
            every { contextProvider.context().getString(R.string.failed_to_get_image) } returns ""
        }

        afterEachTest {
            clearMocks(commandObserver, contextProvider)
        }

        it("should change cover") {
            viewModel.changeCover(mockedUrl)
            assertTrue { viewModel.cover == mockedUrl }
        }

        it("should show toast when cover's uri is null or empty") {
            viewModel.changeCover("")
            val value = viewModel.command.value
            assertTrue {
                viewModel.cover == "" &&
                        value is AlbumCreationViewModel.Command.ShowToast &&
                        value.message == ""
            }
        }
    }

    describe("save of album") {

        val mockedTitle0 = ""
        val mockedArtist0 = ""
        val mockedTitle30 = "abcdefghijklmnoqrstuvwxyzabcdefg"
        val mockedArtist30 = "abcdefghijklmnoqrstuvwxyzabcdefg"
        val mockedTitle = "mockedTitle"
        val mockedArtist = "mockedArtist"

        val mockedAlbumDir: File = mockk(relaxed = true)

        beforeEachTest {
            viewModel.command.observeForever(commandObserver)
            every {
                contextProvider.context().getString(R.string.incorrect_length_toast)
            } returns ""
            every {
                contextProvider.context().getString(R.string.album_size_less_1)
            } returns ""

        }

        afterEachTest {
            clearMocks(commandObserver, contextProvider)
        }

        it("should not save when title's length less then 3") {
            viewModel.saveAlbum(mockedTitle0, mockedArtist)
            verify { commandObserver.onChanged(AlbumCreationViewModel.Command.ShowErrorOnQuery) }
        }

        it("should not save when title's length more then 30") {
            viewModel.saveAlbum(mockedTitle30, mockedArtist)
            verify { commandObserver.onChanged(AlbumCreationViewModel.Command.ShowErrorOnQuery) }
        }

        it("should not save when title's length less then 3") {
            viewModel.saveAlbum(mockedTitle, mockedArtist0)
            verify { commandObserver.onChanged(AlbumCreationViewModel.Command.ShowErrorOnQuery) }
        }

        it("should not save when title's length less more then 30") {
            viewModel.saveAlbum(mockedTitle, mockedArtist30)
            verify { commandObserver.onChanged(AlbumCreationViewModel.Command.ShowErrorOnQuery) }
        }

        it("should not save when checked tracks less then 1") {
            viewModel.saveAlbum(mockedTitle, mockedArtist)
            val value = viewModel.command.value
            assertTrue {
                value is AlbumCreationViewModel.Command.ShowToast &&
                        value.message == ""
            }
        }

//        val mockedTrack: Track = mockk(relaxed = true)
//
//        it("should save album") {
//            val mockedId = 0
//            val trackName = "$mockedArtist-$mockedTitle-$mockedId.mp3"
//
//            every { mockedTrack.id } returns mockedId
//            every { viewModel.checkedTracks } returns listOf(mockedTrack)
//            every { contextProvider.context().getString(R.string.app_local_dir) } returns ""
//            every {
//                contextProvider.context().getExternalFilesDir(Environment.DIRECTORY_MUSIC)
//            } returns mockedAlbumDir
//            every { mockedAlbumDir.exists() } returns false
//            every { trackRepo.fetchTrackPath(anyString()) } returns trackName
//
//            viewModel.saveAlbum(mockedTitle, mockedArtist)
//            verify { commandObserver.onChanged(AlbumCreationViewModel.Command.CloseActivity) }
//        }
    }
})
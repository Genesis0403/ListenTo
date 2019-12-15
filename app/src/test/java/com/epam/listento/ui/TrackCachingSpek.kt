package com.epam.listento.ui

import androidx.lifecycle.Observer
import com.epam.listento.R
import com.epam.listento.ServiceHelper
import com.epam.listento.api.ApiResponse
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.DomainAlbum
import com.epam.listento.domain.DomainArtist
import com.epam.listento.domain.DomainStorage
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.CacheInteractor
import com.epam.listento.model.DownloadInteractor
import com.epam.listento.repository.global.AudioRepository
import com.epam.listento.repository.global.FileRepository
import com.epam.listento.repository.global.StorageRepository
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.utils.ContextProvider
import com.epam.listento.utils.TestDispatchers
import com.epam.listento.utils.emulateInstanteTaskExecutorRule
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@RunWith(JUnitPlatform::class)
object TrackCachingSpek : Spek({

    emulateInstanteTaskExecutorRule()

    val contextProvider: ContextProvider = mockk(relaxed = true)
    val audioRepo: AudioRepository = mockk(relaxed = true)
    val storageRepo: StorageRepository = mockk(relaxed = true)
    val fileRepo: FileRepository = mockk(relaxed = true)
    val trackRepo: TrackRepository = mockk(relaxed = true)
    val db: AppDatabase = mockk(relaxed = true)
    val dao: TracksDao = mockk(relaxed = true)
    val serviceHelper: ServiceHelper = mockk(relaxed = true)
    val dispatchers = TestDispatchers()

    val toastObserver: Observer<String> = mockk(relaxed = true)

    val cacheInteractor = spyk(
        CacheInteractor(
            contextProvider,
            trackRepo,
            db,
            dao
        ),
        recordPrivateCalls = true
    )
    val downloadInteractor = spyk(
        DownloadInteractor(
            contextProvider,
            audioRepo,
            storageRepo,
            fileRepo,
            trackRepo,
            cacheInteractor
        ),
        recordPrivateCalls = true
    )
    val viewModel = MainViewModel(
        contextProvider,
        cacheInteractor,
        downloadInteractor,
        dispatchers,
        serviceHelper
    )

    val mockedId = 1
    val mockedTitle = "A Poem of Integration Testing"
    val mockedName = "Vlad Kondrashkov"
    val trackName = "$mockedName-$mockedTitle-$mockedId.mp3"
    val mockedTrackPath = "somePath"
    val mockedSuccess = "a"
    val mockedFailure = "b"
    val mockedUrl = "url"
    val pathToFile = "pathToFile"
    val errorMessage = "someErrorMessage"

    val mockedException: Exception = mockk(relaxed = true)

    val mockedArtist = DomainArtist(
        id = mockedId,
        name = mockedName,
        cover = null,
        uri = "someUrl"
    )

    val mockedAlbum = DomainAlbum(
        id = mockedId,
        title = mockedTitle,
        artists = listOf(mockedArtist),
        coverUri = "someUrl",
        storageDir = "anotherUrl"
    )

    val mockedTrack = DomainTrack(
        id = mockedId,
        storageDir = "124",
        durationMs = 0L,
        title = mockedTitle,
        artists = listOf(mockedArtist),
        albums = listOf(mockedAlbum),
        timestamp = null
    )

    val mockedStorage = DomainStorage(
        host = "host",
        path = "path",
        s = "s",
        ts = "ts"
    )

    describe("track caching when track is already in files system") {

        beforeEachTest {
            viewModel.showToast.observeForever(toastObserver)

            every {
                contextProvider.context().getString(R.string.success_caching)
            } returns mockedSuccess
            every {
                contextProvider.context().getString(R.string.failed_caching)
            } returns mockedFailure
        }

        afterEachTest {
            clearAllMocks()
        }

        it("should save track into db") {
            coEvery { trackRepo.checkTrackExistence(trackName) } returns true
            every { trackRepo.fetchTrackPath(trackName) } returns mockedTrackPath
            coEvery {
                trackRepo.fetchTrack(mockedId, true)
            } returns ApiResponse.success(mockedTrack)

            viewModel.cacheTrack(mockedId, mockedTitle, mockedName)

            coVerify { cacheInteractor.cacheTrack(mockedId) }
            verify { toastObserver.onChanged(mockedSuccess) }
        }
    }

    describe("track caching when file is not in file system") {

        beforeEachTest {
            viewModel.showToast.observeForever(toastObserver)

            coEvery { trackRepo.checkTrackExistence(trackName) } returns false
            every {
                contextProvider.context().getString(R.string.success_caching)
            } returns mockedSuccess
            every {
                contextProvider.context().getString(R.string.failed_caching)
            } returns mockedFailure
        }

        afterEachTest {
            clearAllMocks()
        }

        it("should show success toast when track is not downloaded") {
            coEvery {
                trackRepo.fetchTrack(
                    mockedId,
                    true
                )
            } returns ApiResponse.success(mockedTrack)
            coEvery { storageRepo.fetchStorage(mockedTrack.storageDir!!) } returns ApiResponse.success(
                mockedStorage
            )
            coEvery { audioRepo.fetchAudioUrl(mockedStorage) } returns mockedUrl
            coEvery { fileRepo.downloadTrack(trackName, mockedUrl) } returns ApiResponse.success(
                pathToFile
            )

            viewModel.cacheTrack(mockedId, mockedTitle, mockedName)

            coVerify(inverse = true) { cacheInteractor.cacheTrack(mockedId) }
            verify { toastObserver.onChanged(mockedSuccess) }
        }

        it("should show error toast when all repos failed") {
            coEvery {
                trackRepo.fetchTrack(
                    mockedId,
                    true
                )
            } returns ApiResponse.error(errorMessage)
            coEvery {
                storageRepo.fetchStorage(mockedTrack.storageDir!!)
            } returns ApiResponse.error(errorMessage)
            coEvery { audioRepo.fetchAudioUrl(mockedStorage) } returns mockedUrl
            coEvery { fileRepo.downloadTrack(trackName, mockedUrl) } returns ApiResponse.error(
                pathToFile
            )

            viewModel.cacheTrack(mockedId, mockedTitle, mockedName)

            coVerify(inverse = true) { cacheInteractor.cacheTrack(mockedId) }
            verify { toastObserver.onChanged(mockedFailure) }
        }

        it("should show error toast when failed to fetch track") {
            coEvery {
                trackRepo.fetchTrack(
                    mockedId,
                    true
                )
            } returns ApiResponse.error(errorMessage)

            viewModel.cacheTrack(mockedId, mockedTitle, mockedName)

            coVerify(inverse = true) { cacheInteractor.cacheTrack(mockedId) }
            verify { toastObserver.onChanged(mockedFailure) }
        }

        it("should show error toast when failed to fetch storage") {
            coEvery {
                trackRepo.fetchTrack(
                    mockedId,
                    true
                )
            } returns ApiResponse.success(mockedTrack)
            coEvery {
                storageRepo.fetchStorage(mockedTrack.storageDir!!)
            } returns ApiResponse.error(mockedException)

            viewModel.cacheTrack(mockedId, mockedTitle, mockedName)

            coVerify(inverse = true) { cacheInteractor.cacheTrack(mockedId) }
            verify { toastObserver.onChanged(mockedFailure) }
        }

        it("should show error toast when file repository failed to download") {
            coEvery {
                trackRepo.fetchTrack(
                    mockedId,
                    true
                )
            } returns ApiResponse.success(mockedTrack)
            coEvery { storageRepo.fetchStorage(mockedTrack.storageDir!!) } returns ApiResponse.success(
                mockedStorage
            )
            coEvery { audioRepo.fetchAudioUrl(mockedStorage) } returns mockedUrl
            coEvery {
                fileRepo.downloadTrack(trackName, mockedUrl)
            } returns ApiResponse.error(mockedException)

            viewModel.cacheTrack(mockedId, mockedTitle, mockedName)

            coVerify(inverse = true) { cacheInteractor.cacheTrack(mockedId) }
            verify { toastObserver.onChanged(mockedFailure) }
        }
    }
})

package com.epam.listento.model

import com.epam.listento.api.ApiResponse
import com.epam.listento.api.Status
import com.epam.listento.domain.DomainStorage
import com.epam.listento.domain.DomainTrack
import com.epam.listento.repository.global.AudioRepository
import com.epam.listento.repository.global.FileRepository
import com.epam.listento.repository.global.StorageRepository
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.utils.ContextProvider
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
object DownloadInteractorSpek : Spek({

    val contextProvider: ContextProvider = mockk(relaxed = true)
    val audioRepo: AudioRepository = mockk(relaxed = true)
    val storageRepo: StorageRepository = mockk(relaxed = true)
    val fileRepo: FileRepository = mockk(relaxed = true)
    val trackRepo: TrackRepository = mockk(relaxed = true)
    val cacheInteractor: CacheInteractor = mockk(relaxed = true)

    val trackResponse: ApiResponse<DomainTrack> = mockk(relaxed = true)
    val storageResponse: ApiResponse<DomainStorage> = mockk(relaxed = true)
    val domainStorage: DomainStorage = mockk(relaxed = true)
    val domainTrack: DomainTrack = mockk(relaxed = true)
    val fileResponse: ApiResponse<String> = mockk(relaxed = true)

    lateinit var interactor: DownloadInteractor

    fun createInteractor() {
        interactor = spyk(
            DownloadInteractor(
                contextProvider,
                audioRepo,
                storageRepo,
                fileRepo,
                trackRepo,
                cacheInteractor
            )
        )
    }

    describe("track downloading") {

        val id = 1
        val title = "title"
        val artist = "artist"
        val trackName = "$artist-$title.mp3"
        val filePath = "somewhere/$trackName"

        beforeEachTest {
            createInteractor()
        }

        afterEachTest {
            clearAllMocks()
        }

        it("should exists") {

            every { trackRepo.checkTrackExistence(trackName) } returns true
            every { trackRepo.fetchTrackPath(trackName) } returns filePath

            runBlocking {
                val response = interactor.downloadTrack(id, title, artist)
                assertTrue { response.status.isSuccess() }
                assertTrue { response.body == filePath }
            }
        }

        it("should fetch track") {

            val storage = "some_storage"
            val url = "someUrl"

            every { trackRepo.checkTrackExistence(trackName) } returns false

            coEvery { trackRepo.fetchTrack(id, true) } returns trackResponse
            every { trackResponse.status } returns Status.SUCCESS
            every { trackResponse.body } returns domainTrack
            every { trackResponse.body?.storageDir } returns storage

            coEvery { storageRepo.fetchStorage(storage) } returns storageResponse
            every { storageResponse.status } returns Status.SUCCESS
            every { storageResponse.body } returns domainStorage

            every { audioRepo.fetchAudioUrl(domainStorage) } returns url

            coEvery { fileRepo.downloadTrack(trackName, url) } returns fileResponse
            every { fileResponse.status } returns Status.SUCCESS
            every { fileResponse.body } returns filePath

            runBlocking {
                val response = interactor.downloadTrack(id, title, artist)
                assertTrue { response.status.isSuccess() }
                assertTrue { response.body == filePath }
            }
        }

        it("should throw error after fetching when status is error") {
            every { trackRepo.checkTrackExistence(trackName) } returns false

            coEvery { trackRepo.fetchTrack(id, true) } returns trackResponse
            every { trackResponse.status } returns Status.ERROR
            every { trackResponse.body } returns domainTrack

            runBlocking {
                val response = interactor.downloadTrack(id, title, artist)
                assertTrue { response.status.isError() }
                assertTrue { response.body == null }
            }
        }

        it("should throw error after fetching when body is null") {
            every { trackRepo.checkTrackExistence(trackName) } returns false

            coEvery { trackRepo.fetchTrack(id, true) } returns trackResponse
            every { trackResponse.status } returns Status.SUCCESS
            every { trackResponse.body } returns null

            runBlocking {
                val response = interactor.downloadTrack(id, title, artist)
                assertTrue { response.status.isError() }
                assertTrue { response.body == null }
            }
        }

        it("should throw error after storage fetching when status is error") {

            val storage = "some_storage"

            every { trackRepo.checkTrackExistence(trackName) } returns false

            coEvery { trackRepo.fetchTrack(id, true) } returns trackResponse
            every { trackResponse.status } returns Status.SUCCESS
            every { trackResponse.body } returns domainTrack
            every { trackResponse.body?.storageDir } returns storage

            coEvery { storageRepo.fetchStorage(storage) } returns storageResponse
            every { storageResponse.status } returns Status.ERROR
            every { storageResponse.body } returns null

            runBlocking {
                val response = interactor.downloadTrack(id, title, artist)
                assertTrue { response.status.isError() }
                assertTrue { response.body == null }
            }
        }

        it("should throw error after storage fetching when body is null") {

            val storage = "some_storage"

            every { trackRepo.checkTrackExistence(trackName) } returns false

            coEvery { trackRepo.fetchTrack(id, true) } returns trackResponse
            every { trackResponse.status } returns Status.SUCCESS
            every { trackResponse.body } returns domainTrack
            every { trackResponse.body?.storageDir } returns storage

            coEvery { storageRepo.fetchStorage(storage) } returns storageResponse
            every { storageResponse.status } returns Status.SUCCESS
            every { storageResponse.body } returns null

            runBlocking {
                val response = interactor.downloadTrack(id, title, artist)
                assertTrue { response.status.isError() }
                assertTrue { response.body == null }
            }
        }

        it("should throw error after downloading when status is error") {

            every { trackRepo.checkTrackExistence(trackName) } returns false

            coEvery { interactor["fetchTrack"](id, true) } returns fileResponse
            every { fileResponse.status } returns Status.ERROR
            every { fileResponse.body } returns null

            runBlocking {
                val response = interactor.downloadTrack(id, title, artist)
                assertTrue { response.status.isError() }
                assertTrue { response.body == null }
            }
        }

        it("should throw error after downloading when body is null") {

            every { trackRepo.checkTrackExistence(trackName) } returns false

            coEvery { interactor["fetchTrack"](id, true) } returns fileResponse
            every { fileResponse.status } returns Status.SUCCESS
            every { fileResponse.body } returns null

            runBlocking {
                val response = interactor.downloadTrack(id, title, artist)
                assertTrue { response.status.isError() }
                assertTrue { response.body == null }
            }
        }
    }
})

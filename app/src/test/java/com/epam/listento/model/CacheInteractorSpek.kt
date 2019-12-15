package com.epam.listento.model

import com.epam.listento.api.ApiResponse
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.DomainTrack
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.utils.ContextProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
object CacheInteractorSpek : Spek({

    val contextProvider = mockk<ContextProvider>(relaxed = true)
    val trackRepo = mockk<TrackRepository>(relaxed = true)
    val db = mockk<AppDatabase>(relaxed = true)
    val dao = mockk<TracksDao>(relaxed = true)

    lateinit var interactor: CacheInteractor

    fun createInteractor() {
        interactor = CacheInteractor(
            contextProvider,
            trackRepo,
            db,
            dao
        )
    }

    describe("track caching") {

        val mockedId = 1
        val mockedTrack = mockk<DomainTrack>(relaxed = true)

        beforeEachTest {
            createInteractor()
        }

        afterEachTest {
            clearMocks(db, dao, contextProvider, trackRepo)
        }

        it("should be in cache") {
            every { mockedTrack.id } returns mockedId
            every { dao.getTracks() } returns listOf(mockedTrack)
            val result = interactor.isTrackInCache(mockedId)
            assertTrue { result }
        }

        it("should not be in cache") {
            every { mockedTrack.id } returns -1
            every { dao.getTracks() } returns listOf(mockedTrack)
            val result = interactor.isTrackInCache(mockedId)
            assertFalse { result }
        }

        it("should cache successfully") {
            coEvery {
                trackRepo.fetchTrack(mockedId, true)
            } returns ApiResponse.success(null)
            runBlocking {
                val result = interactor.cacheTrack(mockedId)
                assertTrue { result }
            }
        }

        it("should failed to cache") {
            coEvery {
                trackRepo.fetchTrack(mockedId, true)
            } returns ApiResponse.error("")
            runBlocking {
                val result = interactor.cacheTrack(mockedId)
                assertFalse { result }
            }
        }
    }
})

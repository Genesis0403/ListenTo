package com.epam.listento.repository

import com.epam.listento.api.Status
import com.epam.listento.api.YandexService
import com.epam.listento.api.model.ApiStorage
import com.epam.listento.utils.MusicMapper
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import retrofit2.Response
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
object StorageRepositoryImplSpek : Spek({

    val service = mockk<YandexService>(relaxed = true)
    val mappers = mockk<MusicMapper>(relaxed = true)

    lateinit var repo: StorageRepositoryImpl

    fun createRepo() {
        repo = StorageRepositoryImpl(
            service,
            mappers
        )
    }

    describe("storage fetching") {

        val storageDir = "storageDir"
        val storage = mockk<ApiStorage>(relaxed = true)
        val mockeBody = mockk<ResponseBody>(relaxed = true)
        val code = 404

        beforeEachTest {
            createRepo()
        }

        afterEachTest {
            clearMocks(service, mappers)
        }

        it("should fetch successfully") {
            coEvery { service.fetchStorage(storageDir) } returns Response.success(storage)

            runBlocking {
                val response = repo.fetchStorage(storageDir)
                assertTrue {
                    response.status == Status.SUCCESS
                }
            }
        }

        it("should failed to fetch") {
            coEvery { service.fetchStorage(storageDir) } returns Response.error(code, mockeBody)

            runBlocking {
                val response = repo.fetchStorage(storageDir)
                assertTrue {
                    response.status == Status.ERROR
                }
            }
        }
    }
})
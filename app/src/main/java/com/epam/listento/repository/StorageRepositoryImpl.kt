package com.epam.listento.repository

import android.util.Log
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.YandexService
import com.epam.listento.domain.DomainStorage
import com.epam.listento.repository.global.StorageRepository
import com.epam.listento.utils.MusicMapper
import javax.inject.Inject

private const val TAG = "STORAGE_REPOSITORY"

class StorageRepositoryImpl @Inject constructor(
    private val service: YandexService,
    private val mappers: MusicMapper
) : StorageRepository {

    override suspend fun fetchStorage(
        storageDir: String
    ): ApiResponse<DomainStorage> {
        return try {
            ApiResponse.success(
                mappers.storageToDomain(
                    service.fetchStorage(storageDir).body()!!
                )
            )
        } catch (e: Throwable) {
            Log.e(TAG, "$e")
            ApiResponse.error(e)
        }
    }
}

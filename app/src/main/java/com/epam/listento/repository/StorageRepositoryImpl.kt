package com.epam.listento.repository

import android.util.Log
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.YandexService
import com.epam.listento.domain.DomainStorage
import com.epam.listento.repository.global.StorageRepository
import com.epam.listento.utils.MusicMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "STORAGE_REPOSITORY"

class StorageRepositoryImpl @Inject constructor(
    private val service: YandexService,
    private val mappers: MusicMapper
) : StorageRepository {

    private var job: Job? = null

    override fun fetchStorage(
        storageDir: String,
        completion: (ApiResponse<DomainStorage>) -> Unit
    ) {
        job?.cancel()
        job = GlobalScope.launch(Dispatchers.IO) {
            try {
                val result =
                    ApiResponse.success(mappers.storageToDomain(service.fetchStorage(storageDir).body()!!))
                completion(result)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
            }
        }
    }
}

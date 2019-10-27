package com.epam.listento.repository.global

import com.epam.listento.api.ApiResponse
import com.epam.listento.domain.DomainStorage

interface StorageRepository {
    suspend fun fetchStorage(storageDir: String): ApiResponse<DomainStorage>
}

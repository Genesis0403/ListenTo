package com.epam.listento.repository

import com.epam.listento.api.ApiResponse
import com.epam.listento.domain.DomainStorage

interface StorageRepository {
    fun fetchStorage(storageDir: String, completion: (ApiResponse<DomainStorage>) -> Unit)
}

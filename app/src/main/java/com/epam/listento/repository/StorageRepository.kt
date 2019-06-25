package com.epam.listento.repository

import com.epam.listento.domain.DomainStorage
import kotlinx.coroutines.Job
import retrofit2.Response

interface StorageRepository {
    fun fetchStorage(storageDir: String, completion: (Response<DomainStorage>) -> Unit): Job
}

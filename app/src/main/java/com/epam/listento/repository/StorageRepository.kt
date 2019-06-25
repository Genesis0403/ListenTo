package com.epam.listento.repository

import com.epam.listento.api.model.ApiStorage
import kotlinx.coroutines.Job
import retrofit2.Response

interface StorageRepository {
    fun fetchStorage(storageDir: String, completion: (Response<ApiStorage>) -> Unit): Job
}

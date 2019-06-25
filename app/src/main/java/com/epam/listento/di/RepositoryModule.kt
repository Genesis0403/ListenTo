package com.epam.listento.di

import android.util.Log
import com.epam.listento.api.YandexService
import com.epam.listento.api.model.ApiStorage
import com.epam.listento.api.model.ApiTrack
import com.epam.listento.repository.StorageRepository
import com.epam.listento.repository.TracksRepository
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Singleton

@Module(
    includes = [ApiModule::class]
)
class RepositoryModule {

    private companion object {
        private const val TRACK = "track"
        private const val TRACKS_REPOSITORY = "TRACKS_REPOSITORY"
        private const val STORAGE_REPOSITORY = "STORAGE_REPOSITORY"
    }

    @Singleton
    @Provides
    fun provideTracksRepo(service: YandexService): TracksRepository {
        return object : TracksRepository {
            override fun fetchTracks(
                text: String,
                completion: (Response<List<ApiTrack>>) -> Unit
            ): Job {
                return GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val request = service.searchTracks(text)
                        val response = if (request.isSuccessful) Response.success(request.body()?.tracks?.items)
                        else Response.error(request.code(), request.errorBody())
                        completion(response)
                    } catch (e: Exception) {
                        Log.e(TRACKS_REPOSITORY, "$e")
                    }
                }
            }
        }
    }

    @Singleton
    @Provides
    fun provideStorageRepo(service: YandexService): StorageRepository {
        return object : StorageRepository {
            override fun fetchStorage(
                storageDir: String,
                completion: (Response<ApiStorage>) -> Unit
            ): Job {
                return GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val result = service.fetchStorage(storageDir)
                        completion(result)
                    } catch (e: Exception) {
                        Log.e(STORAGE_REPOSITORY, "$e")
                    }
                }
            }
        }
    }
}

package com.epam.listento.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.YandexService
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Track
import com.epam.listento.repository.global.TracksRepository
import com.epam.listento.utils.MusicMapper
import com.epam.listento.utils.PlatformMappers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject

class TracksRepositoryImpl @Inject constructor(
    private val service: YandexService,
    private val domainMappers: MusicMapper,
    private val tracksDao: TracksDao,
    private val platformMappers: PlatformMappers
) : TracksRepository {

    override suspend fun getCache(completion: (LiveData<List<Track>>) -> Unit) {
        val tracks =
            tracksDao.getLiveDataTracks().value?.mapNotNull { platformMappers.mapTrack(it) }?.toList()
                ?: emptyList()
        val result = MutableLiveData<List<Track>>().apply {
            value = tracks
        }

        withContext(Dispatchers.Main) {
            completion(result)
        }
    }

    override suspend fun fetchTracks(
        text: String,
        completion: suspend (ApiResponse<List<DomainTrack>>) -> Unit
    ) {
        try {
            val request = service.searchTracks(text)
            val response = if (request.isSuccessful) {
                ApiResponse.success(request.body()?.tracks?.items?.map { domainMappers.trackToDomain(it) })
            } else {
                ApiResponse.error(request.message(), emptyList())
            }
            completion(response)
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            completion(ApiResponse.error(e))
        }
    }

    private companion object {
        private const val TAG = "TRACKS_REPOSITORY"
    }
}

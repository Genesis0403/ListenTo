package com.epam.listento.repository

import android.util.Log
import androidx.annotation.WorkerThread
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
import javax.inject.Inject

class TracksRepositoryImpl @Inject constructor(
    private val service: YandexService,
    private val domainMappers: MusicMapper,
    private val tracksDao: TracksDao,
    private val platformMappers: PlatformMappers
) : TracksRepository {

    @WorkerThread
    override suspend fun getCache(): LiveData<List<Track>> {
        val tracks =
            tracksDao.getLiveDataTracks().value
                ?.mapNotNull { platformMappers.mapTrack(it) }
                ?.toList()
                ?: emptyList()
        return MutableLiveData<List<Track>>().apply {
            value = tracks
        }
    }

    override suspend fun fetchTracks(text: String): ApiResponse<List<DomainTrack>> {
        return try {
            val response = service.searchTracks(text)
            val tracks = response.body()?.tracks
            if (response.isSuccessful && tracks != null) {
                ApiResponse.success(tracks.items?.map {
                    domainMappers.trackToDomain(it)
                })
            } else {
                ApiResponse.error(response.message())
            }
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            ApiResponse.error(e)
        }
    }

    private companion object {
        private const val TAG = "TRACKS_REPOSITORY"
    }
}

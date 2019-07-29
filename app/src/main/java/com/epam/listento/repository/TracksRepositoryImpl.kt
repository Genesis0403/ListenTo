package com.epam.listento.repository

import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.epam.listento.api.YandexService
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Track
import com.epam.listento.repository.global.TracksRepository
import com.epam.listento.utils.MusicMapper
import com.epam.listento.utils.PlatformMappers
import kotlinx.coroutines.*
import retrofit2.Response
import javax.inject.Inject

private const val TAG = "TRACKS_REPOSITORY"

class TracksRepositoryImpl @Inject constructor(
    private val service: YandexService,
    private val domainMappers: MusicMapper,
    private val tracksDao: TracksDao,
    private val platformMappers: PlatformMappers
) : TracksRepository {

    override fun getCache(completion: (LiveData<List<Track>>) -> Unit) {
        GlobalScope.launch(Dispatchers.Default) {
            val tracks =
                tracksDao.getTracks().value?.mapNotNull { platformMappers.mapTrack(it) }?.toList()
                    ?: emptyList()
            val result = MutableLiveData<List<Track>>().apply {
                value = tracks
            }

            withContext(Dispatchers.Main) {
                completion(result)
            }
        }
    }

    override fun fetchTracks(
        text: String,
        completion: (Response<List<DomainTrack>>) -> Unit
    ): Job {
        return GlobalScope.launch(Dispatchers.IO) {
            try {
                val request = service.searchTracks(text)
                val response = if (request.isSuccessful) {
                    Response.success(request.body()?.tracks?.items?.map { domainMappers.trackToDomain(it) })
                } else {
                    Response.error(request.code(), request.errorBody())
                }
                completion(response)
            } catch (e: Exception) {
                Log.e(TAG, "$e")
            }
        }
    }
}

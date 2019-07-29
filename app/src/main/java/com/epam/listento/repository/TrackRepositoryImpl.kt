package com.epam.listento.repository

import android.net.Uri
import android.os.Environment
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.YandexService
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.DomainTrack
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.utils.ContextProvider
import com.epam.listento.utils.MusicMapper
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

private const val LOCAL_DIRECTORY = "ListenToMusic"

class TrackRepositoryImpl @Inject constructor(
    private val contextProvider: ContextProvider,
    private val service: YandexService,
    private val mappers: MusicMapper,
    private val db: AppDatabase,
    private val dao: TracksDao
) : TrackRepository {

    private var job: Job? = null

    override fun fetchTrack(
        id: Int,
        isCaching: Boolean,
        completion: (ApiResponse<DomainTrack>) -> Unit
    ) {
        job?.cancel()
        job = GlobalScope.launch(Dispatchers.IO) {
            val response = service.fetchTrack(id)
            if (response.isSuccessful && response.body() != null) {

                val track = mappers.trackToDomain(response.body()?.track!!)
                if (isCaching) {
                    cacheTrack(track)
                }

                withContext(Dispatchers.Main) {
                    completion(ApiResponse.success(track))
                }
            } else {
                withContext(Dispatchers.Main) {
                    completion(ApiResponse.error(response.message()))
                }
            }
        }
    }

    override fun checkTrackExistence(trackName: String): Boolean {
        val file = getTrackFile(trackName)
        return file.exists()
    }

    override fun fetchTrackUri(trackName: String): Uri {
        if (!checkTrackExistence(trackName)) return Uri.EMPTY
        val file = getTrackFile(trackName)
        return Uri.fromFile(file)
    }

    private fun getTrackFile(trackName: String): File {
        val context = contextProvider.context()
        val dir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            LOCAL_DIRECTORY
        )
        return File(dir, trackName)
    }

    private fun cacheTrack(track: DomainTrack) {
        db.runInTransaction {
            dao.insertTrack(track)
        }
    }
}
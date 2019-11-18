package com.epam.listento.repository

import android.net.Uri
import android.os.Environment
import androidx.annotation.WorkerThread
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.YandexService
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.DomainTrack
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.utils.ContextProvider
import com.epam.listento.utils.MusicMapper
import java.io.File
import java.util.Date
import javax.inject.Inject

private const val LOCAL_DIRECTORY = "ListenToMusic"

class TrackRepositoryImpl @Inject constructor(
    private val contextProvider: ContextProvider,
    private val service: YandexService,
    private val mappers: MusicMapper,
    private val db: AppDatabase,
    private val dao: TracksDao
) : TrackRepository {

    @WorkerThread
    override suspend fun fetchTrack(
        id: Int,
        isCaching: Boolean
    ): ApiResponse<DomainTrack> {
        val response = service.fetchTrack(id)
        return if (response.isSuccessful && response.body() != null) {
            val track = mappers.trackToDomain(response.body()?.track!!)
            if (isCaching) {
                cacheTrack(track)
            }
            ApiResponse.success(track)
        } else {
            ApiResponse.error(response.message())
        }
    }

    override fun checkTrackExistence(trackName: String): Boolean {
        val file = getTrackFile(trackName)
        return file.exists()
    }

    override fun fetchTrackPath(trackName: String): String {
        if (!checkTrackExistence(trackName)) return Uri.EMPTY.toString()
        val file = getTrackFile(trackName)
        return file.path
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
        track.timestamp = Date(System.currentTimeMillis())
        db.runInTransaction {
            dao.insertTrack(track)
        }
    }
}

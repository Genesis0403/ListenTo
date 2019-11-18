package com.epam.listento.model

import android.os.Environment
import android.util.Log
import androidx.annotation.WorkerThread
import com.epam.listento.R
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.utils.ContextProvider
import java.io.File
import javax.inject.Inject

class CacheInteractor @Inject constructor(
    private val contextProvider: ContextProvider,
    private val trackRepo: TrackRepository,
    private val db: AppDatabase,
    private val dao: TracksDao
) {

    @WorkerThread
    suspend fun isTrackInCache(id: Int): Boolean {
        val tracks = dao.getTracks()
        return tracks.any { it.id == id }
    }

    @WorkerThread
    suspend fun cacheTrack(id: Int): Boolean {
        return trackRepo.fetchTrack(id, true).status.isSuccess()
    }

    @WorkerThread
    fun uncacheTrack(id: Int, title: String, artist: String) {
        db.runInTransaction {
            dao.deleteTrackById(id)
            removeTrackFile(id, title, artist)
        }
    }

    @WorkerThread
    fun clearAllCache() {
        db.runInTransaction {
            dao.removeTracks()
            clearFolder()
        }
    }

    @WorkerThread
    private fun clearFolder() {
        val context = contextProvider.context()
        File(
            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
            context.getString(R.string.app_local_dir)
        ).run {
            if (exists()) {
                deleteRecursively()
            }
        }
    }

    private fun removeTrackFile(id: Int, title: String, artist: String) {
        val trackName = "$title-$artist-$id.mp3"
        val file = File(
            Environment.DIRECTORY_MUSIC,
            contextProvider.context().getString(R.string.app_local_dir)
        )
        if (file.delete()
        ) {
            Log.d(TAG, "Successful deletion of: ${file.path}")
        } else {
            Log.d(TAG, "Fail to delete file: ${file.path}")
        }
    }

    private companion object {
        private const val TAG = "CacheInteractor"
    }
}

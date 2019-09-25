package com.epam.listento.model

import android.os.Environment
import com.epam.listento.R
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.utils.ContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class CacheInteractor @Inject constructor(
    private val contextProvider: ContextProvider,
    private val trackRepo: TrackRepository,
    private val db: AppDatabase,
    private val dao: TracksDao
) {

    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var checkJob: Job? = null

    fun isTrackInCache(
        id: Int,
        completion: (isInCache: Boolean) -> Unit
    ) {
        checkJob?.cancel()
        checkJob = cacheScope.launch(Dispatchers.IO) {
            db.runInTransaction {
                val tracks = dao.getTracks()
                val isInCache = tracks.any { it.id == id }
                CoroutineScope(Dispatchers.Main).launch {
                    completion(isInCache)
                }
            }
        }
    }

    fun cacheTrack(id: Int, completion: (isSuccess: Boolean) -> Unit) {
        cacheScope.launch(Dispatchers.IO) {
            trackRepo.fetchTrack(id, true) { response ->
                withContext(Dispatchers.Main) {
                    completion(response.status.isSuccess())
                }
            }
        }
    }

    fun uncacheTrack(id: Int) {
        cacheScope.launch(Dispatchers.IO) {
            db.runInTransaction {
                dao.deleteTrackById(id)
            }
        }
    }

    fun clearAllCache() {
        cacheScope.launch(Dispatchers.IO) {
            db.runInTransaction {
                dao.removeTracks()
                clearFolder()
            }
        }
    }

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
}

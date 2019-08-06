package com.epam.listento.model

import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
import com.epam.listento.repository.global.TrackRepository
import kotlinx.coroutines.*
import javax.inject.Inject

class CacheInteractor @Inject constructor(
    private val trackRepo: TrackRepository,
    private val db: AppDatabase,
    private val dao: TracksDao
) {

    private var checkJob: Job? = null

    fun isTrackInCache(
        id: Int,
        completion: (isInCache: Boolean) -> Unit
    ) {
        checkJob?.cancel()
        checkJob = GlobalScope.launch(Dispatchers.IO) {
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
        GlobalScope.launch(Dispatchers.IO) {
            trackRepo.fetchTrack(id, true) { response ->
                withContext(Dispatchers.Main) {
                    completion(response.status.isSuccess())
                }
            }
        }
    }

    fun uncacheTrack(id: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            db.runInTransaction {
                dao.deleteTrackById(id)
            }
        }
    }

    fun clearAllCache() {
        GlobalScope.launch(Dispatchers.IO) {
            db.runInTransaction {
                dao.removeTracks()
            }
        }
    }
}
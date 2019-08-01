package com.epam.listento.model

import android.support.v4.media.MediaMetadataCompat
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
import com.epam.listento.model.player.utils.id
import kotlinx.coroutines.*
import javax.inject.Inject

class CacheInteractor @Inject constructor(
    private val db: AppDatabase,
    private val dao: TracksDao
) {

    private var cacheJob: Job? = null

    fun isTrackInCache(
        track: MediaMetadataCompat,
        completion: (isInCache: Boolean) -> Unit
    ) {
        cacheJob?.cancel()
        cacheJob = GlobalScope.launch(Dispatchers.IO) {
            db.runInTransaction {
                val tracks = dao.getTracks()
                val isInCache = tracks.any { it.id == track.id?.toInt() }
                CoroutineScope(Dispatchers.Main).launch {
                    completion(isInCache)
                }
            }
        }
    }
}
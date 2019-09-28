package com.epam.listento.ui.viewmodels

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.epam.listento.R
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.mapTrack
import com.epam.listento.db.TracksDao
import com.epam.listento.model.CacheInteractor
import com.epam.listento.model.DownloadInteractor
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.player.PlaybackState.PAUSED
import com.epam.listento.model.player.PlaybackState.PLAYING
import com.epam.listento.model.player.PlaybackState.STOPPED
import com.epam.listento.model.player.PlaybackState.UNKNOWN
import com.epam.listento.model.toMetadata
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.repository.global.TracksRepository
import com.epam.listento.ui.nightmode.ThemeLiveData
import com.epam.listento.utils.ContextProvider
import com.epam.listento.utils.PlatformMappers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val contextProvider: ContextProvider,
    private val tracksRepo: TracksRepository,
    private val musicRepo: MusicRepository,
    private val cacheInteractor: CacheInteractor,
    private val downloadInteractor: DownloadInteractor,
    dao: TracksDao,
    mappers: PlatformMappers
) : ViewModel() {

    val lastQuery: MutableLiveData<String> = MutableLiveData()
    private var job: Job? = null
    private var playbackJob: Job? = null

    private val _tracks: MutableLiveData<ApiResponse<List<Track>>> = MutableLiveData()
    val tracks: LiveData<ApiResponse<List<Track>>> get() = _tracks

    private val sp = PreferenceManager.getDefaultSharedPreferences(contextProvider.context())
    val nightMode = ThemeLiveData(contextProvider.context(), sp)

    val cachedTracks: LiveData<List<Track>> =
        Transformations.switchMap(dao.getLiveDataTracks()) { domain ->
            MutableLiveData<List<Track>>().apply {
                value = if (domain.isNullOrEmpty()) {
                    emptyList()
                } else {
                    domain.mapNotNull { mappers.mapTrack(it) }
                }
            }
        }

    fun fetchTracks(text: String) {
        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            tracksRepo.fetchTracks(text) { response ->
                if (response.isSuccessful) {
                    val items =
                        response.body()?.asSequence()?.map { mapTrack(it) }?.filterNotNull()
                            ?.toList()
                    _tracks.postValue(ApiResponse.success(items))
                } else {
                    _tracks.postValue(ApiResponse.error(response.message()))
                }
            }
        }
    }

    fun cacheTrack(id: Int, title: String, artist: String) {
        downloadInteractor.downloadTrack(id, title, artist) { response ->
            val context = contextProvider.context()
            val message = if (response.status.isSuccess()) {
                context.getString(R.string.success_caching)
            } else {
                context.getString(R.string.failed_caching)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun uncacheTrack(id: Int, title: String, atist: String) {
        cacheInteractor.uncacheTrack(id)
    }

    fun clearCache() {
        cacheInteractor.clearAllCache()
    }

    fun itemClick(track: Track, list: List<Track>) {
        val metadata = list.map { it.toMetadata() }
        musicRepo.run {
            setSource(metadata)
            setCurrent(track.toMetadata())
        }
    }

    fun cachePlaybackChange(
        id: Int,
        state: PlaybackState
    ) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch(Dispatchers.Default) {
            val newRes = when (state) {
                PLAYING -> R.drawable.exo_icon_pause
                PAUSED -> R.drawable.exo_icon_play
                STOPPED -> Track.NO_RES
                UNKNOWN -> Track.NO_RES
            }

            cachedTracks as MutableLiveData
            val result = cachedTracks.value?.map { track ->
                val resId = if (track.id == id) newRes else Track.NO_RES
                track.copy(res = resId)
            }
            cachedTracks.postValue(result)
        }
    }

    fun searchPlaybackChange(
        id: Int,
        state: PlaybackState
    ) {
        // TODO REFACTOR!!!
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch(Dispatchers.Default) {
            val newRes = when (state) {
                PLAYING -> R.drawable.exo_icon_pause
                PAUSED -> R.drawable.exo_icon_play
                STOPPED -> Track.NO_RES
                UNKNOWN -> Track.NO_RES
            }

            val result = _tracks.value?.body?.map { track ->
                val resId = if (track.id == id) newRes else Track.NO_RES
                track.copy(res = resId)
            }
            _tracks.postValue(ApiResponse.success(result))
        }
    }
}

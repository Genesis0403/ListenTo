package com.epam.listento.ui.viewmodels

import androidx.lifecycle.*
import com.epam.listento.R
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.mapTrack
import com.epam.listento.db.TracksDao
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.player.PlaybackState.*
import com.epam.listento.model.player.utils.id
import com.epam.listento.model.toMetadata
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.repository.global.TracksRepository
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
    dao: TracksDao,
    mappers: PlatformMappers
) : ViewModel() {

    val lastQuery: MutableLiveData<String> = MutableLiveData()
    private var job: Job? = null
    private var playbackJob: Job? = null

    private val _tracks: MutableLiveData<ApiResponse<List<Track>>> = MutableLiveData()
    val tracks: LiveData<ApiResponse<List<Track>>> get() = _tracks

    val cachedTracks: LiveData<List<Track>> = Transformations.switchMap(dao.getLiveDataTracks()) { domain ->
        val tracks = domain.mapNotNull { mappers.mapTrack(it) }.toList()
        MutableLiveData<List<Track>>().apply {
            value = tracks
        }
    }

    fun fetchTracks(text: String) {
        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            tracksRepo.fetchTracks(text) { response ->
                if (response.isSuccessful) {
                    val items =
                        response.body()?.asSequence()?.map { mapTrack(it) }?.filterNotNull()?.toList()
                    _tracks.postValue(ApiResponse.success(items))
                } else {
                    _tracks.postValue(ApiResponse.error(response.message()))
                }
            }
        }
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

    fun searchPlaybackChange( //TODO REFACTOR!!!
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

            tracks as MutableLiveData
            val result = tracks.value?.body?.map { track ->
                val resId = if (track.id == id) newRes else Track.NO_RES
                track.copy(res = resId)
            }
            _tracks.postValue(ApiResponse.success(result))
        }
    }
}

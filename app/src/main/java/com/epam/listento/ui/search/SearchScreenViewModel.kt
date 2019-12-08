package com.epam.listento.ui.search

import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epam.listento.R
import com.epam.listento.ServiceHelper
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.Status
import com.epam.listento.api.mapTrack
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.toMetadata
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.repository.global.TracksRepository
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.BaseViewModelFactory
import com.epam.listento.utils.SingleLiveEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider

class SearchScreenViewModel @Inject constructor(
    val serviceHelper: ServiceHelper,
    private val tracksRepo: TracksRepository,
    private val musicRepo: MusicRepository,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val _tracks: MutableLiveData<ApiResponse<List<Track>>> = MutableLiveData()
    val tracks: LiveData<ApiResponse<List<Track>>> get() = _tracks

    private val _command: MutableLiveData<Command> = SingleLiveEvent()
    val command: LiveData<Command> get() = _command

    @UiThread
    fun fetchTracks(query: String) {
        if (query.isNotEmpty() && query.length <= QUERY_LENGTH_MAX) {
            viewModelScope.launch(dispatchers.default) {
                tracksRepo.fetchTracks(query).also {
                    mapTracksAndPublish(it)
                }
            }
        } else {
            _command.value = Command.StopLoading
        }
    }

    @UiThread
    fun handleItemClick(track: Track) {
        viewModelScope.launch(dispatchers.ui) {
            val current = serviceHelper.currentPlaying.value
            val state = serviceHelper.playbackState.value
            _command.value = if (state != PlaybackState.Stopped &&
                state != PlaybackState.Paused &&
                current == track.id
            ) {
                Command.ShowPlayerActivity
            } else {
                changePlaylistAndSetCurrent(track)
                Command.PlayTrack
            }
        }
    }

    @UiThread
    fun handleLongItemClick(track: Track) {
        val artist = track.artist?.name ?: ""
        _command.value =
            Command.ShowCacheDialog(
                track.id,
                track.title,
                artist
            )
    }

    private suspend fun changePlaylistAndSetCurrent(track: Track) {
        withContext(dispatchers.default) {
            tracks.value?.body?.map { it.toMetadata() }?.let { metadata ->
                withContext(dispatchers.ui) {
                    musicRepo.setSource(metadata)
                    musicRepo.setCurrent(track.toMetadata())
                }
            }
        }
    }

    @UiThread
    fun handlePlayerStateChange(trackId: Int = -1) {
        viewModelScope.launch(dispatchers.default) {
            val newRes = when (serviceHelper.playbackState.value) {
                PlaybackState.Playing -> R.drawable.exo_icon_pause
                PlaybackState.Paused -> R.drawable.exo_icon_play
                else -> Track.NO_RES
            }

            val result = tracks.value?.body?.map { track ->
                val resId = if (track.id == trackId) newRes else Track.NO_RES
                track.copy(res = resId)
            }
            _tracks.postValue(ApiResponse.success(result))
        }
    }

    private fun mapTracksAndPublish(response: ApiResponse<List<DomainTrack>>) {
        val result = if (response.status == Status.SUCCESS) {
            val items = response.body?.run {
                asSequence()
                    .mapNotNull { mapTrack(it) }
                    .toList()
            } ?: emptyList()
            ApiResponse.success(items)
        } else {
            ApiResponse.error(response.error)
        }
        _tracks.postValue(result)
    }

    sealed class Command {
        object ShowPlayerActivity : Command()
        object PlayTrack : Command()
        object StopLoading : Command()
        class ShowCacheDialog(
            val id: Int,
            val title: String,
            val artist: String
        ) : Command()
    }

    class Factory @Inject constructor(
        provider: Provider<SearchScreenViewModel>
    ) : ViewModelProvider.Factory by BaseViewModelFactory(provider)

    private companion object {
        private const val QUERY_LENGTH_MAX = 30
    }
}
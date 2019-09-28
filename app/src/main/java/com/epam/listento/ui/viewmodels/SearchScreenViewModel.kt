package com.epam.listento.ui.viewmodels

import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epam.listento.R
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.mapTrack
import com.epam.listento.model.Track
import com.epam.listento.model.toMetadata
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.repository.global.TracksRepository
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.utils.SingleLiveEvent
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class SearchScreenViewModel @Inject constructor(
    private val tracksRepo: TracksRepository,
    private val musicRepo: MusicRepository
) : ViewModel() {

    private val _tracks: MutableLiveData<ApiResponse<List<Track>>> = MutableLiveData()
    val tracks: LiveData<ApiResponse<List<Track>>> get() = _tracks

    private val _currentPlaying: MutableLiveData<Track> = MutableLiveData()
    val currentPlaying: LiveData<Track> get() = _currentPlaying

    private val _playbackState: MutableLiveData<PlaybackState> = MutableLiveData()
    val playbackState: LiveData<PlaybackState> get() = _playbackState

    private val _navigationAction: MutableLiveData<NavigationAction> = SingleLiveEvent()
    val navigationActions: LiveData<NavigationAction> get() = _navigationAction

    fun fetchTracks(query: String) {
        viewModelScope.launch {
            tracksRepo.fetchTracks(query) { response ->
                if (response.isSuccessful) {
                    val items = response.body()
                        ?.asSequence()
                        ?.map { mapTrack(it) }
                        ?.filterNotNull()
                        ?.toList()
                    _tracks.postValue(ApiResponse.success(items))
                } else {
                    _tracks.postValue(ApiResponse.error(response.message()))
                }
            }
        }
    }

    fun handleItemClick(track: Track) {
        val current = currentPlaying.value
        val state = playbackState.value
        _navigationAction.value = if (state != PlaybackState.Stopped &&
            state != PlaybackState.Paused &&
            current?.id == track.id
        ) {
            NavigationAction.PlayerActivity
        } else {
            NavigationAction.ShouldChangePlaylist(track)
        }
    }

    fun handleLongItemClick(track: Track) {
        val artist = track.artist?.name ?: ""
        _navigationAction.value = NavigationAction.NeedCacheDialog(track.id, track.title, artist)
    }

    fun changePlaylistAndSetCurrent(track: Track) {
        viewModelScope.launch {
            tracks.value?.body?.map { it.toMetadata() }?.let { metadata ->
                musicRepo.setSource(metadata)
                musicRepo.setCurrent(track.toMetadata())
            }
        }
    }

    fun handleMetadataChange(trackId: Int) {
        viewModelScope.launch {
            _currentPlaying.value = _tracks.value?.body?.find { it.id == trackId } ?: return@launch
        }
    }

    fun handlePlaybackStateChange(state: Int) {
        _playbackState.value = when (state) {
            PlaybackStateCompat.STATE_PLAYING -> PlaybackState.Playing
            PlaybackStateCompat.STATE_PAUSED -> PlaybackState.Paused
            PlaybackStateCompat.STATE_STOPPED -> PlaybackState.Stopped
            else -> PlaybackState.None
        }
    }

    fun handlePlayerStateChange(trackId: Int) {
        viewModelScope.launch {
            val newRes = when (playbackState.value) {
                PlaybackState.Playing -> R.drawable.exo_icon_pause
                PlaybackState.Paused -> R.drawable.exo_icon_play
                else -> Track.NO_RES
            }

            val result = _tracks.value?.body?.map { track ->
                val resId = if (track.id == trackId) newRes else Track.NO_RES
                track.copy(res = resId)
            }
            _tracks.postValue(ApiResponse.success(result))
        }
    }

    sealed class NavigationAction {
        object PlayerActivity : NavigationAction()
        data class ShouldChangePlaylist(val track: Track) : NavigationAction()
        data class NeedCacheDialog(
            val id: Int,
            val title: String,
            val artist: String
        ) : NavigationAction()
    }

    class Factory @Inject constructor(
        private val provider: Provider<SearchScreenViewModel>
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return provider.get() as T
        }
    }
}
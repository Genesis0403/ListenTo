package com.epam.listento.ui.cache

import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epam.listento.R
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.toMetadata
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.PlatformMappers
import com.epam.listento.utils.SingleLiveEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider

class CacheScreenViewModel @Inject constructor(
    private val musicRepo: MusicRepository,
    private val mappers: PlatformMappers,
    private val dispatchers: AppDispatchers,
    dao: TracksDao
) : ViewModel() {

    private val _currentPlaying = MutableLiveData<Track>()
    val currentPlaying: LiveData<Track> get() = _currentPlaying

    private val _playbackState = MutableLiveData<PlaybackState>()
    val playbackState: LiveData<PlaybackState> get() = _playbackState

    private val _command: MutableLiveData<Command> = SingleLiveEvent()
    val command: LiveData<Command> get() = _command

    private val _tracks: MutableLiveData<List<Track>> =
        Transformations.switchMap(dao.getLiveDataTracks()) { domain ->
            MutableLiveData<List<Track>>().apply {
                value = mapTracksToPlatform(domain)
            }
        } as MutableLiveData
    val tracks: LiveData<List<Track>> get() = _tracks

    fun handleItemClick(track: Track) {
        viewModelScope.launch(dispatchers.ui) {
            val current = currentPlaying.value
            val state = playbackState.value
            _command.value = if (state != PlaybackState.Stopped &&
                state != PlaybackState.Paused &&
                current?.id == track.id
            ) {
                Command.ShowPlayerActivity
            } else {
                changePlaylistAndPlayCurrent(track)
                Command.PlayTrack
            }
        }
    }

    fun handleLongItemClick(track: Track) {
        // TODO create playlist on long click and add dots menu for cache actions
        val artist = track.artist?.name ?: ""
        _command.value =
            Command.ShowCacheDialog(
                track.id,
                track.title,
                artist
            )
    }

    fun handleMetadataChange(trackId: Int) {
        viewModelScope.launch(dispatchers.ui) {
            _currentPlaying.value = withContext(dispatchers.default) {
                tracks.value?.find { it.id == trackId }
            } ?: return@launch
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

    fun handlePlayerStateChange(trackId: Int = -1) {
        viewModelScope.launch(dispatchers.default) {
            val newRes = getPlaybackRes()

            val result = _tracks.value?.map { track ->
                val resId = if (track.id == trackId) newRes else Track.NO_RES
                track.copy(res = resId)
            }
            _tracks.postValue(result)
        }
    }

    private suspend fun changePlaylistAndPlayCurrent(track: Track) {
        withContext(dispatchers.default) {
            _tracks.value?.map { it.toMetadata() }?.let { metadata ->
                withContext(dispatchers.ui) {
                    musicRepo.setSource(metadata)
                    musicRepo.setCurrent(track.toMetadata())
                }
            }
        }
    }

    private fun getPlaybackRes(): Int {
        return when (playbackState.value) {
            PlaybackState.Playing -> R.drawable.exo_icon_pause
            PlaybackState.Paused -> R.drawable.exo_icon_play
            else -> Track.NO_RES
        }
    }

    private fun mapTracksToPlatform(tracks: List<DomainTrack?>): List<Track> {
        return if (tracks.isNullOrEmpty()) {
            emptyList()
        } else {
            tracks.mapNotNull {
                val track = mappers.mapTrack(it)
                if (track == currentPlaying.value) {
                    track?.res = getPlaybackRes()
                }
                track
            }
        }
    }

    sealed class Command {
        object ShowPlayerActivity : Command()
        object PlayTrack : Command()
        class ShowCacheDialog(
            val id: Int,
            val title: String,
            val artist: String
        ) : Command()
    }

    class Factory @Inject constructor(
        private val provider: Provider<CacheScreenViewModel>
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return provider.get() as T
        }
    }
}

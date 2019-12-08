package com.epam.listento.ui.albums

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epam.listento.R
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.toMetadata
import com.epam.listento.repository.global.AlbumsRepository
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.ContextProvider
import com.epam.listento.utils.SingleLiveEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumViewModel(
    private val contextProvider: ContextProvider,
    private val dispatchers: AppDispatchers,
    private val musicRepo: MusicRepository,
    val title: String,
    albumsRepo: AlbumsRepository,
    id: Int
) : ViewModel() {

    private val _currentPlaying = MutableLiveData<Track>()
    val currentPlaying: LiveData<Track> get() = _currentPlaying

    private val _playbackState = MutableLiveData<PlaybackState>()
    val playbackState: LiveData<PlaybackState> get() = _playbackState

    private val _tracks: MutableLiveData<List<Track>> =
        Transformations.switchMap(albumsRepo.getAlbumById(id)) {
            musicRepo.setSource(convertToMetadata(it.tracks))
            MutableLiveData(it.tracks)
        } as MutableLiveData
    val tracks: LiveData<List<Track>> get() = _tracks

    private val _command: MutableLiveData<Command> = SingleLiveEvent()
    val command: LiveData<Command> get() = _command

    @UiThread
    fun handleClick(track: Track) {
        _command.value = if (playbackState.value == PlaybackState.Playing &&
            currentPlaying.value?.id == track.id
        ) {
            Command.PauseTrack
        } else {
            musicRepo.setCurrent(track.toMetadata())
            Command.PlayTrack
        }
    }

    @UiThread
    fun handleMetadataChange(trackId: Int) {
        viewModelScope.launch(dispatchers.ui) {
            _currentPlaying.value = withContext(dispatchers.default) {
                tracks.value?.find { it.id == trackId }
            } ?: return@launch
        }
    }

    @UiThread
    fun handlePlaybackStateChange(state: Int) {
        _playbackState.value = when (state) {
            PlaybackStateCompat.STATE_PLAYING -> PlaybackState.Playing
            PlaybackStateCompat.STATE_PAUSED -> PlaybackState.Paused
            PlaybackStateCompat.STATE_STOPPED -> PlaybackState.Stopped
            else -> PlaybackState.None
        }
    }

    @UiThread
    fun handlePlayerStateChange(trackId: Int = -1) {
        viewModelScope.launch(dispatchers.default) {
            val newRes = getPlaybackRes()

            val result = tracks.value?.map { track ->
                val resId = if (track.id == trackId) newRes else Track.NO_RES
                track.copy(res = resId)
            }
            _tracks.postValue(result)
        }
    }

    private fun convertToMetadata(tracks: List<Track>): List<MediaMetadataCompat> {
        return tracks.map { it.toMetadata() }.toList()
    }

    private fun getPlaybackRes(): Int {
        return when (playbackState.value) {
            PlaybackState.Playing -> R.drawable.lt_pause_icon
            PlaybackState.Paused -> R.drawable.lt_play_icon
            else -> Track.NO_RES
        }
    }

    sealed class Command {
        object PauseTrack : Command()
        object PlayTrack : Command()
    }

    private companion object {
        const val TAG = "AlbumViewModel"
    }
}
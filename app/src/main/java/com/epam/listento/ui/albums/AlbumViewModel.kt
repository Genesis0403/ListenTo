package com.epam.listento.ui.albums

import android.support.v4.media.MediaMetadataCompat
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epam.listento.R
import com.epam.listento.ServiceHelper
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.toMetadata
import com.epam.listento.repository.global.AlbumsRepository
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.SingleLiveEvent
import kotlinx.coroutines.launch

class AlbumViewModel(
    private val serviceHelper: ServiceHelper,
    private val dispatchers: AppDispatchers,
    private val musicRepo: MusicRepository,
    val title: String,
    albumsRepo: AlbumsRepository,
    id: Int
) : ViewModel() {

    val tracks: LiveData<List<Track>> get() = _tracks
    val command: LiveData<Command> get() = _command

    val currentPlaying: LiveData<Int> get() = serviceHelper.currentPlaying
    val playbackState: LiveData<PlaybackState> get() = serviceHelper.playbackState
    val transportControls get() = serviceHelper.transportControls

    private val _command: MutableLiveData<Command> = SingleLiveEvent()
    private val _tracks: MutableLiveData<List<Track>> =
        Transformations.switchMap(albumsRepo.getAlbumById(id)) {
            musicRepo.setSource(convertToMetadata(it.tracks))
            MutableLiveData(it.tracks)
        } as MutableLiveData

    @UiThread
    fun handleClick(track: Track) {
        _command.value = if (serviceHelper.playbackState.value == PlaybackState.Playing &&
            serviceHelper.currentPlaying.value == track.id
        ) {
            Command.PauseTrack
        } else {
            musicRepo.setCurrent(track.toMetadata())
            Command.PlayTrack
        }
    }

    @UiThread
    fun handlePlayerStateChange(trackId: Int = -1) {
        viewModelScope.launch(dispatchers.default) {
            val newRes = getPlaybackRes()

            val result = tracks.value?.map { track ->
                val resId = if (track.id == trackId)
                    newRes
                else
                    Track.NO_RES
                track.copy(res = resId)
            }
            _tracks.postValue(result)
        }
    }

    private fun convertToMetadata(tracks: List<Track>): List<MediaMetadataCompat> {
        return tracks.map { it.toMetadata() }.toList()
    }

    private fun getPlaybackRes(): Int {
        return when (serviceHelper.playbackState.value) {
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
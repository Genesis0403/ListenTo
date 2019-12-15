package com.epam.listento.ui.cache

import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epam.listento.R
import com.epam.listento.ServiceHelper
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.CustomAlbum
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.toMetadata
import com.epam.listento.repository.global.AlbumsRepository
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.PlatformMappers
import com.epam.listento.utils.SingleLiveEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider

class CacheScreenViewModel @Inject constructor(
    private val serviceHelper: ServiceHelper,
    private val musicRepo: MusicRepository,
    private val mappers: PlatformMappers,
    private val dispatchers: AppDispatchers,
    albumsRepo: AlbumsRepository,
    dao: TracksDao
) : ViewModel() {

    val currentPlaying: LiveData<Int> get() = serviceHelper.currentPlaying
    val playbackState: LiveData<PlaybackState> get() = serviceHelper.playbackState
    val transportControls = serviceHelper.transportControls

    val command: LiveData<Command> get() = _command
    val albums: LiveData<List<CustomAlbum>> = albumsRepo.getAlbums()
    val tracks: LiveData<List<Track>> get() = _tracks

    private val _command: MutableLiveData<Command> = SingleLiveEvent()
    private val _tracks: MutableLiveData<List<Track>> =
        Transformations.switchMap(dao.getLiveDataTracks()) { domain ->
            MutableLiveData<List<Track>>(mapTracksToPlatform(domain))
        } as MutableLiveData

    @UiThread
    fun handleTrackClick(track: Track) {
        viewModelScope.launch(dispatchers.ui) {
            val current = serviceHelper.currentPlaying.value
            val state = serviceHelper.playbackState.value
            _command.value = if (state != PlaybackState.Stopped &&
                state != PlaybackState.Paused &&
                current == track.id
            ) {
                Command.ShowPlayerActivity
            } else {
                changePlaylistAndPlayCurrent(track)
                Command.PlayTrack
            }
        }
    }

    @UiThread
    fun handleAlbumLongClick(album: CustomAlbum) {
        _command.value = Command.ShowAlbumDialog(album.id)
    }

    @UiThread
    fun handleAlbumClick(album: CustomAlbum) {
        _command.value = Command.ShowAlbumActivity(album.title, album.id, album.cover)
    }

    @UiThread
    fun handleThreeDotButtonClick(track: Track) {
        val artist = track.artist?.name ?: ""
        _command.value =
            Command.ShowCacheDialog(
                track.id,
                track.title,
                artist
            )
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

    private suspend fun changePlaylistAndPlayCurrent(track: Track) {
        val metadata = withContext(dispatchers.default) {
            tracks.value?.map { it.toMetadata() }
        } ?: return
        musicRepo.setSource(metadata)
        musicRepo.setCurrent(track.toMetadata())
    }

    private fun getPlaybackRes(): Int {
        return when (serviceHelper.playbackState.value) {
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
                if (track?.id == serviceHelper.currentPlaying.value) {
                    track?.res = getPlaybackRes()
                }
                track
            }
        }
    }

    sealed class Command {
        object ShowPlayerActivity : Command()

        object PlayTrack : Command()

        class ShowAlbumDialog(id: Int) : Command()

        class ShowAlbumActivity(
            val title: String,
            val id: Int,
            val cover: String
        ) : Command()

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

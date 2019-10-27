package com.epam.listento.ui.player

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.player.utils.albumCover
import com.epam.listento.model.player.utils.artist
import com.epam.listento.model.player.utils.duration
import com.epam.listento.model.player.utils.title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Timer
import javax.inject.Inject
import javax.inject.Provider
import kotlin.concurrent.schedule

class PlayerViewModel @Inject constructor() : ViewModel() {

    private val _currentPlaying = MutableLiveData<MetadataTrack>().also {
        it.value = MetadataTrack()
    }
    val currentPlaying: LiveData<MetadataTrack> get() = _currentPlaying

    private val _playbackState = MutableLiveData<PlaybackState>().also {
        it.value = PlaybackState.None
    }
    val playbackState: LiveData<PlaybackState> get() = _playbackState

    private var timer: Timer? = null
    private var job: Job? = null

    fun startScheduler(action: () -> Unit) {
        timer?.let { return }
        timer = Timer().also {
            it.schedule(DELAY, DURATION_SECOND) {
                job = viewModelScope.launch(Dispatchers.Main) {
                    action()
                }
            }.run()
        }
    }

    fun stopScheduler() {
        job?.cancel()
        timer?.cancel()
        timer = null
    }

    fun handleMetadataChange(track: MetadataTrack) {
        _currentPlaying.value = track
    }

    fun handlePlaybackStateChange(state: Int) {
        val result = when (state) {
            PlaybackStateCompat.STATE_PLAYING -> PlaybackState.Playing
            PlaybackStateCompat.STATE_PAUSED -> PlaybackState.Paused
            PlaybackStateCompat.STATE_STOPPED -> PlaybackState.Stopped
            else -> PlaybackState.None
        }
        if (result != playbackState.value) {
            _playbackState.value = result
        }
    }

    fun handlePlayButtonClick() {
        // TODO
    }

    fun handleForwardButton() {
        // TODO
    }

    fun handleRewindButton() {
        // TODO
    }

    data class MetadataTrack(
        val title: String = NOT_DEFINED_TITLE,
        val artist: String = NOT_DEFINED_TITLE,
        val duration: Long = 0,
        val cover: String = ""
    )

    sealed class PlayerAction {
        object ShouldPlay : PlayerAction()
        object ShouldPause : PlayerAction()
        object ShouldSkipForward : PlayerAction()
        object ShouldSkipBackward : PlayerAction()
    }

    class Factory @Inject constructor(
        private val provider: Provider<PlayerViewModel>
    ) : ViewModelProvider.Factory {

        @Suppress("UNCKECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return provider.get() as T
        }
    }

    private companion object {
        private const val TAG = "PlayerViewModel"
        private const val NOT_DEFINED_TITLE = "None"
        private const val DURATION_SECOND: Long = 1000
        private const val DELAY: Long = 0
    }
}

fun MediaMetadataCompat.toMetadataTrack(): PlayerViewModel.MetadataTrack {
    return PlayerViewModel.MetadataTrack(
        title,
        artist,
        duration,
        albumCover
    )
}

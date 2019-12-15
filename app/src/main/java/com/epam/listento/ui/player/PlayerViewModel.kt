package com.epam.listento.ui.player

import android.support.v4.media.MediaMetadataCompat
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epam.listento.R
import com.epam.listento.ServiceHelper
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.player.utils.albumCover
import com.epam.listento.model.player.utils.artist
import com.epam.listento.model.player.utils.duration
import com.epam.listento.model.player.utils.title
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Timer
import javax.inject.Inject
import javax.inject.Provider
import kotlin.concurrent.schedule

class PlayerViewModel @Inject constructor(
    private val serviceHelper: ServiceHelper,
    private val musicRepo: MusicRepository
) : ViewModel() {

    val currentTrack get() = musicRepo.getCurrent().toMetadataTrack()
    val transportControls get() = serviceHelper.transportControls
    val progress get() = serviceHelper.progressMs

    val currentPlaying: LiveData<Int> get() = serviceHelper.currentPlaying
    val playbackState: LiveData<PlaybackState> get() = serviceHelper.playbackState

    private val _command: MutableLiveData<Command> = SingleLiveEvent()
    val command: LiveData<Command> get() = _command

    private var timer: Timer? = null
    private var job: Job? = null

    @UiThread
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

    @UiThread
    fun stopScheduler() {
        job?.cancel()
        timer?.cancel()
        timer = null
    }

    @UiThread
    fun handleMediaButtonClick(id: Int) {
        _command.value = when (id) {
            R.id.playButton ->
                if (serviceHelper.playbackState.value == PlaybackState.Playing) {
                    Command.Pause
                } else {
                    Command.Play
                }
            R.id.forwardButton -> Command.Forward
            R.id.rewindButton -> Command.Backward
            else -> Command.None
        }
    }

    data class MetadataTrack(
        val title: String = NOT_DEFINED_TITLE,
        val artist: String = NOT_DEFINED_TITLE,
        val duration: Long = 0,
        val cover: String = ""
    )

    sealed class Command {
        object Play : Command()
        object Pause : Command()
        object Forward : Command()
        object Backward : Command()
        object None : Command()
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

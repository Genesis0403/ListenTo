package com.epam.listento.ui.albums

import android.content.Context
import android.os.Environment
import androidx.annotation.MainThread
import androidx.annotation.UiThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epam.listento.R
import com.epam.listento.model.CustomAlbum
import com.epam.listento.model.Track
import com.epam.listento.repository.global.AlbumsRepository
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.BaseViewModelFactory
import com.epam.listento.utils.ContextProvider
import com.epam.listento.utils.SingleLiveEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Provider

class AlbumCreationViewModel @Inject constructor(
    private val albumsRepo: AlbumsRepository,
    private val trackRepo: TrackRepository,
    private val dispatchers: AppDispatchers,
    private val contextProvider: ContextProvider
) : ViewModel() {

    private val context: Context by lazy(LazyThreadSafetyMode.NONE) {
        contextProvider.context()
    }

    private val _command: MutableLiveData<Command> = SingleLiveEvent()
    val command: LiveData<Command> get() = _command

    private val _checkedTracks: MutableList<Track> = mutableListOf()
    val checkedTracks: List<Track> get() = _checkedTracks

    var cover: String = ""
        private set

    @UiThread
    fun onMenuItemClick(itemId: Int): Boolean {
        return when (itemId) {
            R.id.saveItem -> {
                _command.value =
                    Command.SaveAlbum
                true
            }
            R.id.addImage -> {
                _command.value =
                    Command.ChangeCover
                true
            }
            android.R.id.home -> {
                _command.value =
                    Command.CloseActivity
                true
            }
            else -> false
        }
    }

    @UiThread
    fun onTrackClick(track: Track) {
        if (_checkedTracks.contains(track)) {
            _checkedTracks.remove(track)
        } else {
            _checkedTracks.add(track)
        }
    }

    @UiThread
    fun changeCover(url: String?) {
        _command.value = if (url.isNullOrEmpty()) {
            Command.ShowToast(context.getString(R.string.failed_to_get_image))
        } else {
            cover = url
            Command.LoadImage(cover)
        }
    }

    @UiThread
    fun saveAlbum(title: String, artist: String) {
        if (isNotValidTitleAndArtistLength(title.length, artist.length)) {
            _command.value = Command.ShowErrorOnQuery
            return
        }
        if (_checkedTracks.isEmpty()) {
            _command.value = Command.ShowToast(context.getString(R.string.album_size_less_1))
            return
        }
        viewModelScope.launch(dispatchers.ui) {
            val childDir = "${context.getString(R.string.app_local_dir)}/$title-$artist/"
            checkedTracks.forEach {
                val trackName = "${it.artist?.name}-${it.title}-${it.id}.mp3"
                val albumDir = getAlbumDir(childDir) ?: return@launch
                val path = withContext(dispatchers.default) { trackRepo.fetchTrackPath(trackName) }
                File(path).copyTo(albumDir, overwrite = true)
                addAlbumIntoCache(title, artist)
            }
        }
    }

    private fun getAlbumDir(childDir: String): File? {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), childDir)
        return if (dir.exists()) null else dir
    }

    private suspend fun addAlbumIntoCache(title: String, artist: String) {
        withContext(dispatchers.io) {
            albumsRepo.addAlbum(
                CustomAlbum(title, artist, cover, _checkedTracks)
            )
        }
        _command.value = Command.CloseActivity
    }

    private fun isNotValidTitleAndArtistLength(title: Int, artist: Int): Boolean {
        return title !in TITLE_MIN_LENGTH..TITLE_MAX_LENGTH ||
            artist !in ARTIST_MIN_LENGTH..ARTIST_MAX_LENGTH
    }

    sealed class Command {
        object SaveAlbum : Command()
        object ChangeCover : Command()
        object CloseActivity : Command()
        object ShowErrorOnQuery : Command()
        class LoadImage(val uri: String) : Command()
        class ShowToast(val message: String) : Command()
    }

    class Factory @Inject constructor(
        provider: Provider<AlbumCreationViewModel>
    ) : ViewModelProvider.Factory by BaseViewModelFactory(provider)

    private companion object {
        private const val TAG = "AlbumCreationViewModel"
        private const val TITLE_MAX_LENGTH = 30
        private const val ARTIST_MAX_LENGTH = 30
        private const val TITLE_MIN_LENGTH = 3
        private const val ARTIST_MIN_LENGTH = 3
    }
}

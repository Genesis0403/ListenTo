package com.epam.listento.ui.viewmodels

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.epam.listento.R
import com.epam.listento.model.CacheInteractor
import com.epam.listento.model.DownloadInteractor
import com.epam.listento.utils.ContextProvider
import javax.inject.Inject
import javax.inject.Provider

class MainViewModel @Inject constructor(
    private val contextProvider: ContextProvider,
    private val cacheInteractor: CacheInteractor,
    private val downloadInteractor: DownloadInteractor
) : ViewModel() {

    private val _nightMode: MutableLiveData<Int> = MutableLiveData()
    val nightMode: LiveData<Int> get() = _nightMode

    fun handleThemeChange(isNightMode: Boolean, key: String) {
        if (key == NIGHT_MODE_KEY) {
            _nightMode.value = if (isNightMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        }
    }

    fun cacheTrack(id: Int, title: String, artist: String) {
        downloadInteractor.downloadTrack(id, title, artist) { response ->
            val context = contextProvider.context()
            val message = if (response.status.isSuccess()) {
                context.getString(R.string.success_caching)
            } else {
                context.getString(R.string.failed_caching)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun uncacheTrack(id: Int, title: String, artist: String) {
        cacheInteractor.uncacheTrack(id)
    }

    class Factory @Inject constructor(
        private val provider: Provider<MainViewModel>
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return provider.get() as T
        }
    }

    private companion object {
        private const val NIGHT_MODE_KEY = "night_mode"
    }
}

package com.epam.listento.ui

import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epam.listento.R
import com.epam.listento.model.CacheInteractor
import com.epam.listento.model.DownloadInteractor
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.BaseViewModelFactory
import com.epam.listento.utils.ContextProvider
import com.epam.listento.utils.SingleLiveEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Provider

class MainViewModel @Inject constructor(
    private val contextProvider: ContextProvider,
    private val cacheInteractor: CacheInteractor,
    private val downloadInteractor: DownloadInteractor,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    private val _nightMode: MutableLiveData<Int> = SingleLiveEvent()
    val nightMode: LiveData<Int> get() = _nightMode

    private val _showToast: MutableLiveData<String> = SingleLiveEvent()
    val showToast: LiveData<String> get() = _showToast

    @UiThread
    fun handleThemeChange(isNightMode: Boolean, key: String) {
        if (key == NIGHT_MODE_KEY) {
            _nightMode.value = if (isNightMode) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        }
    }

    @UiThread
    fun cacheTrack(id: Int, title: String, artist: String) {
        viewModelScope.launch(dispatchers.ui) {
            val response = withContext(dispatchers.io) {
                downloadInteractor.downloadTrack(id, title, artist)
            }
            val context = contextProvider.context()
            _showToast.value = if (response.status.isSuccess()) {
                context.getString(R.string.success_caching)
            } else {
                context.getString(R.string.failed_caching)
            }
        }
    }

    @UiThread
    fun uncacheTrack(id: Int, title: String, artist: String) {
        viewModelScope.launch(dispatchers.io) {
            cacheInteractor.uncacheTrack(id, title, artist)
        }
    }

    class Factory @Inject constructor(
        provider: Provider<MainViewModel>
    ) : ViewModelProvider.Factory by BaseViewModelFactory(provider)

    private companion object {
        private const val NIGHT_MODE_KEY = "night_mode"
    }
}

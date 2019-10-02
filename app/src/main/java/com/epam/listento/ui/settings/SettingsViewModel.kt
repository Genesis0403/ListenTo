package com.epam.listento.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.epam.listento.model.CacheInteractor
import com.epam.listento.model.DownloadInteractor
import javax.inject.Inject
import javax.inject.Provider

class SettingsViewModel @Inject constructor(
    private val cacheInteractor: CacheInteractor,
    private val downloadInteractor: DownloadInteractor
) : ViewModel() {

    fun clearCache() {
        cacheInteractor.clearAllCache()
    }

    class Factory @Inject constructor(
        private val provider: Provider<SettingsViewModel>
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return provider.get() as T
        }
    }
}

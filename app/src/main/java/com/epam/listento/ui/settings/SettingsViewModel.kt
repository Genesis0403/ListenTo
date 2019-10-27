package com.epam.listento.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.epam.listento.model.CacheInteractor
import com.epam.listento.model.DownloadInteractor
import com.epam.listento.utils.AppDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class SettingsViewModel @Inject constructor(
    private val cacheInteractor: CacheInteractor,
    private val dispatchers: AppDispatchers
) : ViewModel() {

    fun clearCache() {
        viewModelScope.launch(dispatchers.io) {
            cacheInteractor.clearAllCache()
        }
    }

    class Factory @Inject constructor(
        private val provider: Provider<SettingsViewModel>
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return provider.get() as T
        }
    }
}

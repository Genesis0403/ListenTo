package com.epam.listento.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Provider

@Suppress("UNCHECKED_CAST")
class BaseViewModelFactory<T : ViewModel>(private val provider: Provider<T>) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return provider.get() as T
    }
}
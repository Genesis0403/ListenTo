package com.epam.listento.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.epam.listento.ServiceHelper
import com.epam.listento.repository.global.AlbumsRepository
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.ContextProvider
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject

@Suppress("UNCHECKED_CAST")
class AlbumViewModelFactory @AssistedInject constructor(
    private val serviceHelper: ServiceHelper,
    private val contextProvider: ContextProvider,
    private val dispatchers: AppDispatchers,
    private val musicRepo: MusicRepository,
    private val albumsRepo: AlbumsRepository,
    @Assisted val title: String,
    @Assisted val id: Int
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AlbumViewModel(
            serviceHelper,
            contextProvider,
            dispatchers,
            musicRepo,
            title,
            albumsRepo,
            id
        ) as T
    }

    @AssistedInject.Factory
    interface Factory {
        fun create(title: String, id: Int): AlbumViewModelFactory
    }
}
package com.epam.listento.repository.global

import androidx.lifecycle.LiveData
import com.epam.listento.model.Album
import com.epam.listento.model.CustomAlbum

interface AlbumsRepository {
    fun getAlbums(): LiveData<List<CustomAlbum>>
    suspend fun addAlbum(album: CustomAlbum)
    suspend fun removeAlbum(id: Int)
    suspend fun addAlbums(albums: List<CustomAlbum>)
}
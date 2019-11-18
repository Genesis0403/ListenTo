package com.epam.listento.repository

import androidx.lifecycle.LiveData
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.CustomAlbumsDao
import com.epam.listento.model.CustomAlbum
import com.epam.listento.repository.global.AlbumsRepository
import javax.inject.Inject

class AlbumsRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val dao: CustomAlbumsDao
) : AlbumsRepository {

    override suspend fun addAlbums(albums: List<CustomAlbum>) {
        db.runInTransaction {
            dao.insertAlbums(albums)
        }
    }

    override fun getAlbums(): LiveData<List<CustomAlbum>> {
        return dao.getAlbums()
    }

    override suspend fun addAlbum(album: CustomAlbum) {
        db.runInTransaction {
            dao.insertAlbum(album)
        }
    }

    override suspend fun removeAlbum(id: Int) {
        dao.removeAlbum(id)
    }
}
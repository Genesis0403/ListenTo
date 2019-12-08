package com.epam.listento.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.epam.listento.model.CustomAlbum

@Dao
interface CustomAlbumsDao {

    @Query("SELECT * FROM CustomAlbum")
    fun getAlbums(): LiveData<List<CustomAlbum>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAlbums(albums: List<CustomAlbum>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAlbum(album: CustomAlbum)

    @Query("DELETE FROM CustomAlbum WHERE id = :id")
    fun removeAlbum(id: Int)

    @Query("DELETE FROM CustomAlbum")
    fun removeAlbums()

    @Query("SELECT * FROM CustomAlbum WHERE title = :title")
    fun getAlbumByTitle(title: String): LiveData<List<CustomAlbum>>

    @Query("SELECT * FROM CustomAlbum WHERE id = :id")
    fun getAlbumById(id: Int): LiveData<CustomAlbum>
}
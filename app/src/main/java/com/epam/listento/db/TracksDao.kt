package com.epam.listento.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.epam.listento.model.Track

@Dao
interface TracksDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrack(track: Track)

    @Query("SELECT * FROM Track")
    fun getTracks(): LiveData<List<Track>>

    @Query("DELETE FROM Track where id = :id")
    fun deleteTrackById(id: Int)
}

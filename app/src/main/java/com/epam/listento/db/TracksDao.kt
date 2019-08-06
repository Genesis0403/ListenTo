package com.epam.listento.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.epam.listento.domain.DomainTrack

@Dao
interface TracksDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrack(track: DomainTrack)

    @Query("SELECT * FROM DomainTrack ORDER BY timestamp DESC")
    fun getLiveDataTracks(): LiveData<List<DomainTrack>>

    @Query("SELECT * FROM DomainTrack")
    fun getTracks(): List<DomainTrack>

    @Query("DELETE FROM DomainTrack WHERE id = :id")
    fun deleteTrackById(id: Int)

    @Query("DELETE FROM DomainTrack")
    fun removeTracks()
}

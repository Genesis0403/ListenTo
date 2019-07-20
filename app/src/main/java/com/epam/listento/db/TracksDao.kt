package com.epam.listento.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Track

@Dao
interface TracksDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrack(track: DomainTrack)

    @Query("SELECT * FROM DomainTrack")
    fun getTracks(): LiveData<List<DomainTrack>>

    @Query("DELETE FROM DomainTrack where id = :id")
    fun deleteTrackById(id: Int)
}

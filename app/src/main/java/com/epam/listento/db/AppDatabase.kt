package com.epam.listento.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.Track

@Database(
    entities = [
        DomainTrack::class
    ],
    version = 2
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tracksDao(): TracksDao
}

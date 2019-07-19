package com.epam.listento.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.epam.listento.model.Track

@Database(
    entities = [
        Track::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tracksDao(): TracksDao
}

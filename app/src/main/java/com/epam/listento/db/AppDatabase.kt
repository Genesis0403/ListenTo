package com.epam.listento.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.CustomAlbum

@Database(
    entities = [
        DomainTrack::class,
        CustomAlbum::class
    ],
    version = 3
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tracksDao(): TracksDao
    abstract fun customAlbumsDao(): CustomAlbumsDao
}

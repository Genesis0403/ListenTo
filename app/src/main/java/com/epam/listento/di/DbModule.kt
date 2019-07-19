package com.epam.listento.di

import android.app.Application
import androidx.room.Room
import com.epam.listento.R
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DbModule {

    @Singleton
    @Provides
    fun provideDB(context: Application): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, context.getString(R.string.dbName))
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun provideTracksDao(db: AppDatabase): TracksDao {
        return db.tracksDao()
    }
}

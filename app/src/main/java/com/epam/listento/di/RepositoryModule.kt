package com.epam.listento.di

import com.epam.listento.repository.*
import com.epam.listento.repository.global.*
import com.epam.listento.utils.MusicMapper
import com.epam.listento.utils.PlatformMappers
import com.epam.listento.utils.mappers.MusicMappersImpl
import com.epam.listento.utils.mappers.PlatformMappersImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module(
    includes = [
        ApiModule::class,
        DbModule::class
    ]
)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun provideTracksRepo(tracksRepo: TracksRepositoryImpl): TracksRepository

    @Singleton
    @Binds
    abstract fun provideStorageRepo(storageRepo: StorageRepositoryImpl): StorageRepository

    @Singleton
    @Binds
    abstract fun provideAudioRepository(audioRepo: AudioRepositoryImpl): AudioRepository

    @Singleton
    @Binds
    abstract fun provideFileRepository(fileRepo: FileRepositoryImpl): FileRepository

    @Singleton
    @Binds
    abstract fun provideMusicRepo(musicRepo: MusicRepositoryImpl): MusicRepository

    @Singleton
    @Binds
    abstract fun provideTrackRepo(trackRepo: TrackRepositoryImpl): TrackRepository

    @Singleton
    @Binds
    abstract fun providePlatformMappers(platformMappersImpl: PlatformMappersImpl): PlatformMappers

    @Singleton
    @Binds
    abstract fun provideMusicMappers(musicMappers: MusicMappersImpl): MusicMapper
}

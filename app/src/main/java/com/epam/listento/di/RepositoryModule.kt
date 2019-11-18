package com.epam.listento.di

import com.epam.listento.repository.AlbumsRepositoryImpl
import com.epam.listento.repository.AudioRepositoryImpl
import com.epam.listento.repository.FileRepositoryImpl
import com.epam.listento.repository.MusicRepositoryImpl
import com.epam.listento.repository.StorageRepositoryImpl
import com.epam.listento.repository.TrackRepositoryImpl
import com.epam.listento.repository.TracksRepositoryImpl
import com.epam.listento.repository.global.AlbumsRepository
import com.epam.listento.repository.global.AudioRepository
import com.epam.listento.repository.global.FileRepository
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.repository.global.StorageRepository
import com.epam.listento.repository.global.TrackRepository
import com.epam.listento.repository.global.TracksRepository
import com.epam.listento.utils.MusicMapper
import com.epam.listento.utils.PlatformMappers
import com.epam.listento.utils.mappers.MusicMappersImpl
import com.epam.listento.utils.mappers.PlatformMappersImpl
import dagger.Binds
import dagger.Module
import dagger.Reusable
import javax.inject.Singleton

@Module(
    includes = [
        ApiModule::class,
        DbModule::class
    ]
)
abstract class RepositoryModule {

    @Reusable
    @Binds
    abstract fun bindTracksRepo(tracksRepo: TracksRepositoryImpl): TracksRepository

    @Reusable
    @Binds
    abstract fun bindStorageRepo(storageRepo: StorageRepositoryImpl): StorageRepository

    @Reusable
    @Binds
    abstract fun bindAudioRepository(audioRepo: AudioRepositoryImpl): AudioRepository

    @Reusable
    @Binds
    abstract fun bindFileRepository(fileRepo: FileRepositoryImpl): FileRepository

    @Reusable
    @Binds
    abstract fun bindMusicRepo(musicRepo: MusicRepositoryImpl): MusicRepository

    @Reusable
    @Binds
    abstract fun bindTrackRepo(trackRepo: TrackRepositoryImpl): TrackRepository

    @Reusable
    @Binds
    abstract fun bindPlatformMappers(platformMappersImpl: PlatformMappersImpl): PlatformMappers

    @Reusable
    @Binds
    abstract fun bindMusicMappers(musicMappers: MusicMappersImpl): MusicMapper

    @Reusable
    @Binds
    abstract fun bindCustomAlbumsRepository(repo: AlbumsRepositoryImpl): AlbumsRepository
}

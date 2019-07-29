package com.epam.listento.di

import android.app.Application
import com.epam.listento.api.YandexService
import com.epam.listento.api.model.*
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.*
import com.epam.listento.model.Album
import com.epam.listento.model.Artist
import com.epam.listento.model.Track
import com.epam.listento.model.player.ListSource
import com.epam.listento.model.player.MusicSource
import com.epam.listento.repository.*
import com.epam.listento.repository.global.*
import com.epam.listento.utils.ContextProvider
import com.epam.listento.utils.MusicMapper
import com.epam.listento.utils.PlatformMappers
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    includes = [
        ApiModule::class,
        DbModule::class
    ]
)
class RepositoryModule {

    @Singleton
    @Provides
    fun provideExoPlayer(app: Application): SimpleExoPlayer {
        return ExoPlayerFactory.newSimpleInstance(app)
    }

    @Singleton
    @Provides
    fun provideTracksRepo(
        service: YandexService,
        domainMappers: MusicMapper,
        tracksDao: TracksDao,
        platformMappers: PlatformMappers
    ): TracksRepository {
        return TracksRepositoryImpl(
            service,
            domainMappers,
            tracksDao,
            platformMappers
        )
    }

    @Singleton
    @Provides
    fun provideStorageRepo(
        service: YandexService,
        mappers: MusicMapper
    ): StorageRepository {
        return StorageRepositoryImpl(service, mappers)
    }

    @Singleton
    @Provides
    fun providePlatformMappers(): PlatformMappers {
        return object : PlatformMappers {
            override fun mapTrack(track: DomainTrack?): Track? {
                return track?.let {
                    Track(
                        track.id,
                        track.durationMs ?: 0,
                        track.title ?: "N/A",
                        mapArtist(track.artists?.firstOrNull()),
                        track.storageDir ?: "",
                        mapAlbum(track.albums?.firstOrNull())
                    )
                }
            }

            override fun mapAlbum(album: DomainAlbum?): Album? {
                return album?.let {
                    val listCover = mapUrl(album.coverUri ?: "", "100x100")
                    val albumCover = mapUrl(album.coverUri ?: "", "700x700")
                    Album(
                        album.id,
                        listCover,
                        albumCover,
                        album.title ?: "N/A",
                        mapArtist(album.artists?.firstOrNull())
                    )
                }
            }

            override fun mapArtist(artist: DomainArtist?): Artist? {
                return artist?.let {
                    val thumbnailUrl = mapUrl(artist.uri ?: "", "100x100")
                    val coverUrl = mapUrl(artist.uri ?: "", "700x700")
                    Artist(
                        artist.id,
                        thumbnailUrl,
                        coverUrl,
                        artist.name ?: "N/A"
                    )
                }
            }

            override fun mapUrl(url: String, replacement: String): String {
                return "https://" + url.replace("%%", replacement)
            }
        }
    }

    @Singleton
    @Provides
    fun provideMusicMappers(): MusicMapper {
        return object : MusicMapper {
            override fun trackToDomain(track: ApiTrack): DomainTrack {
                return DomainTrack(
                    track.id,
                    track.storageDir,
                    track.durationMs,
                    track.title,
                    track.albums?.map { albumToDomain(it) },
                    track.artists?.map { artistToDomain(it) }
                )
            }

            override fun storageToDomain(storage: ApiStorage): DomainStorage {
                return DomainStorage(
                    storage.host,
                    storage.path,
                    storage.ts,
                    storage.s
                )
            }

            override fun artistToDomain(artist: ApiArtist): DomainArtist {
                return DomainArtist(
                    artist.id,
                    artist.uri,
                    artist.name,
                    artist.cover?.let { coverToDomain(it) }
                )
            }

            override fun albumToDomain(album: ApiAlbum): DomainAlbum {
                return DomainAlbum(
                    album.id,
                    album.coverUri,
                    album.storageDir,
                    album.title,
                    album.artists?.map { artistToDomain(it) }
                )
            }

            override fun coverToDomain(cover: ApiCover): DomainCover {
                return DomainCover(cover.uri)
            }
        }
    }

    @Singleton
    @Provides
    fun provideAudioRepository(): AudioRepository {
        return AudioRepositoryImpl()
    }

    @Singleton
    @Provides
    fun provideFileRepository(
        service: YandexService,
        contextProvider: ContextProvider
    ): FileRepository {
        return FileRepositoryImpl(service, contextProvider)
    }

    @Singleton
    @Provides
    fun provideMusicRepo(): MusicRepository {
        return MusicRepositoryImpl()
    }

    @Singleton
    @Provides
    fun provideTrackRepo(
        contextProvider: ContextProvider,
        service: YandexService,
        mappers: MusicMapper,
        db: AppDatabase,
        dao: TracksDao
    ): TrackRepository {
        return TrackRepositoryImpl(
            contextProvider,
            service,
            mappers,
            db,
            dao
        )
    }
}

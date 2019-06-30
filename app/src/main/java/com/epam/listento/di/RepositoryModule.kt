package com.epam.listento.di

import android.util.Log
import com.epam.listento.api.YandexService
import com.epam.listento.api.model.*
import com.epam.listento.domain.*
import com.epam.listento.repository.StorageRepository
import com.epam.listento.repository.TracksRepository
import com.epam.listento.utils.MusicMapper
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Singleton

@Module(
    includes = [ApiModule::class]
)
class RepositoryModule {

    private companion object {
        private const val TRACKS_REPOSITORY = "TRACKS_REPOSITORY"
        private const val STORAGE_REPOSITORY = "STORAGE_REPOSITORY"
    }

    @Singleton
    @Provides
    fun provideTracksRepo(
        service: YandexService,
        mappers: MusicMapper
    ): TracksRepository {
        return object : TracksRepository {
            override fun fetchTracks(
                text: String,
                completion: (Response<List<DomainTrack>>) -> Unit
            ): Job {
                return GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val request = service.searchTracks(text)
                        val response = if (request.isSuccessful) {
                            Response.success(request.body()?.tracks?.items?.map { mappers.trackToDomain(it) })
                        } else {
                            Response.error(request.code(), request.errorBody())
                        }
                        completion(response)
                    } catch (e: Exception) {
                        Log.e(TRACKS_REPOSITORY, "$e")
                    }
                }
            }
        }
    }

    @Singleton
    @Provides
    fun provideStorageRepo(
        service: YandexService,
        mappers: MusicMapper
    ): StorageRepository {
        return object : StorageRepository {
            override fun fetchStorage(
                storageDir: String,
                completion: (Response<DomainStorage>) -> Unit
            ): Job {
                return GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val result = Response.success(mappers.storageToDomain(service.fetchStorage(storageDir).body()!!))
                        completion(result)
                    } catch (e: Exception) {
                        Log.e(STORAGE_REPOSITORY, "$e")
                    }
                }
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
}

package com.epam.listento.di

import android.app.Application
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.YandexService
import com.epam.listento.api.model.*
import com.epam.listento.domain.*
import com.epam.listento.model.NotificationTrack
import com.epam.listento.model.Track
import com.epam.listento.repository.*
import com.epam.listento.utils.MusicMapper
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Singleton

@Module(
    includes = [
        ApiModule::class
    ]
)
class RepositoryModule {

    private companion object {
        private const val TRACKS_REPOSITORY = "TRACKS_REPOSITORY"
        private const val STORAGE_REPOSITORY = "STORAGE_REPOSITORY"
        private const val FILE_REPOSITORY = "FILE_REPOSITORY"
        private const val PREFIX_PLACEHOLDER = "track_placeholder"
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

            private var job: Job? = null

            override fun fetchStorage(
                storageDir: String,
                completion: (Response<DomainStorage>) -> Unit
            ) {
                job?.cancel()
                job = GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val result =
                            Response.success(mappers.storageToDomain(service.fetchStorage(storageDir).body()!!))
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

    @Singleton
    @Provides
    fun provideAudioRepository(): AudioRepository {
        return object : AudioRepository {
            override fun fetchAudioUrl(storage: DomainStorage): String {
                return StringBuilder("https://").apply {
                    append(storage.host)
                    append("/get-mp3/")
                    append(storage.s)
                    append("/")
                    append(storage.ts)
                    append(storage.path)
                }.toString()
            }
        }
    }

    @Singleton
    @Provides
    fun provideFileRepository(service: YandexService, context: Application): FileRepository {
        return object : FileRepository {

            private var job: Job? = null

            override fun downloadTrack(
                audioUrl: String,
                completion: (Response<Uri>) -> Unit
            ) {
                job?.cancel()
                job = GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val response = service.downloadTrack(audioUrl)
                        if (response.isSuccessful) {
                            val url = response.body()?.let {
                                downloadFile(it, context)
                            }
                            completion(Response.success(url))
                        } else {
                            completion(Response.error(response.code(), response.errorBody()))
                        }
                    } catch (e: Exception) {
                        Log.e(FILE_REPOSITORY, "$e")
                    }
                }
            }

            private fun downloadFile(response: ResponseBody, context: Context): Uri {
                val file = File.createTempFile(PREFIX_PLACEHOLDER, null, context.cacheDir)
                try {
                    val fileReader = ByteArray(4096)
                    response.byteStream().use { inputStream ->
                        FileOutputStream(file).use { outStream ->
                            while (true) {
                                val read = inputStream.read(fileReader)
                                if (read == -1) {
                                    break
                                }
                                outStream.write(fileReader, 0, read)
                            }
                            outStream.flush()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(FILE_REPOSITORY, "$e")
                }
                return Uri.fromFile(file)
            }
        }
    }

    @Singleton
    @Provides
    fun provideMusicRepo(
        storageRepository: StorageRepository,
        audioRepository: AudioRepository,
        fileRepository: FileRepository
    ): MusicRepository {
        return object : MusicRepository {

            private val tracks = mutableListOf<Track>()
            private var current = 0

            override fun isDataChanged(data: List<Track>): Boolean {
                return !tracks.containsAll(data)
            }

            override fun containsTrack(track: Track): Boolean {
                return tracks.contains(track)
            }

            override fun getCurrent(): Track {
                return tracks[current]
            }

            override fun setCurrent(track: Track) {
                current = if (tracks.contains(track)) {
                    tracks.indexOf(track)
                } else {
                    return
                }
            }

            override fun getNext(): Track {
                current = ++current % tracks.size
                return tracks[current]
            }

            override fun getPrevious(): Track {
                val previous = --current
                return if (previous > -1) {
                    tracks[previous]
                } else {
                    current = tracks.size - 1
                    tracks[current]
                }
            }

            override fun setSource(data: List<Track>) {
                with(tracks) {
                    clear()
                    addAll(data)
                    current = 0
                }
            }

            override fun downloadTrack(track: NotificationTrack, completion: (ApiResponse<Uri>) -> Unit) {
                storageRepository.fetchStorage(track.storageDir) { response ->
                    if (response.isSuccessful) {
                        response.body()?.let {
                            val downloadUrl = audioRepository.fetchAudioUrl(it)
                            onDownloadResponse(downloadUrl) { result ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    completion(result)
                                }
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            completion(ApiResponse.error(response.message()))
                        }
                    }
                }
            }

            private fun onDownloadResponse(downloadUrl: String, completion: (ApiResponse<Uri>) -> Unit) {
                fileRepository.downloadTrack(downloadUrl) { response ->
                    completion(
                        if (response.isSuccessful) {
                            ApiResponse.success(response.body())
                        } else {
                            ApiResponse.error(response.message())
                        }
                    )
                }
            }
        }
    }

    @Singleton
    @Provides
    fun provideTrackRepo(service: YandexService): TrackRepository {
        return object : TrackRepository {

            private var job: Job? = null

            override fun fecthTrack(
                id: Int,
                completion: (ApiResponse<NotificationTrack>) -> Unit
            ) {
                job?.cancel()
                job = GlobalScope.launch(Dispatchers.IO) {
                    val response = service.fetchTrack(id)
                    val track = mapToNotification(response.body()?.track!!)
                    CoroutineScope(Dispatchers.Main).launch {
                        if (response.isSuccessful) {
                            completion(ApiResponse.success(track))
                        } else {
                            completion(ApiResponse.error(response.message()))
                        }
                    }
                }
            }

            private fun mapToNotification(track: ApiTrack): NotificationTrack {
                val apiUrl = track.albums?.first()?.coverUri ?: track.artists?.first()?.cover?.uri ?: ""
                val url = URL("https://${apiUrl.replace("%%", "700x700")}")
                val bitmap = BitmapFactory.decodeStream(url.openStream())
                return track.run {
                    NotificationTrack(
                        id,
                        storageDir ?: "",
                        title ?: "None",
                        artists?.first()?.name ?: "None",
                        durationMs ?: 0,
                        bitmap
                    )
                }
            }
        }
    }
}

package com.epam.listento.di

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.YandexService
import com.epam.listento.api.model.*
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
import com.epam.listento.domain.*
import com.epam.listento.model.Album
import com.epam.listento.model.Artist
import com.epam.listento.model.Track
import com.epam.listento.repository.*
import com.epam.listento.utils.MusicMapper
import com.epam.listento.utils.PlatformMappers
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import javax.inject.Singleton

@Module(
    includes = [
        ApiModule::class,
        DbModule::class
    ]
)
class RepositoryModule {

    private companion object {
        private const val TRACKS_REPOSITORY = "TRACKS_REPOSITORY"
        private const val STORAGE_REPOSITORY = "STORAGE_REPOSITORY"
        private const val FILE_REPOSITORY = "FILE_REPOSITORY"
        private const val PREFIX_PLACEHOLDER = "track_placeholder"
        private const val LOCAL_DIRECTORY = "ListenToMusic"
    }

    @Singleton
    @Provides
    fun provideTracksRepo(
        service: YandexService,
        domainMappers: MusicMapper,
        db: AppDatabase,
        tracksDao: TracksDao,
        platformMappers: PlatformMappers
    ): TracksRepository {
        return object : TracksRepository {

            override fun getCache(completion: (LiveData<List<Track>>) -> Unit) {
                GlobalScope.launch(Dispatchers.Default) {
                    val tracks =
                        tracksDao.getTracks().value?.mapNotNull { platformMappers.mapTrack(it) }?.toList()
                            ?: emptyList()
                    val result = MutableLiveData<List<Track>>().apply {
                        value = tracks
                    }

                    withContext(Dispatchers.Main) {
                        completion(result)
                    }
                }
            }

            override fun fetchTracks(
                text: String,
                completion: (Response<List<DomainTrack>>) -> Unit
            ): Job {
                return GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val request = service.searchTracks(text)
                        val response = if (request.isSuccessful) {
                            Response.success(request.body()?.tracks?.items?.map { domainMappers.trackToDomain(it) })
                        } else {
                            Response.error(request.code(), request.errorBody())
                        }
                        completion(response)
                    } catch (e: Exception) {
                        Log.e(TRACKS_REPOSITORY, "$e")
                    }
                }
            }

            override fun cacheTrack(track: Track) {
                GlobalScope.launch(Dispatchers.IO) {
                    db.runInTransaction {
                        //TODO implement fetching domain track and cache it
                    }
                }
            }

            override fun uncacheTrack(track: Track) {
                GlobalScope.launch(Dispatchers.IO) {
                    db.runInTransaction {
                        tracksDao.deleteTrackById(track.id)
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
                completion: (ApiResponse<DomainStorage>) -> Unit
            ) {
                job?.cancel()
                job = GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val result =
                            ApiResponse.success(mappers.storageToDomain(service.fetchStorage(storageDir).body()!!))
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
    fun provideFileRepository(
        service: YandexService,
        context: Application
    ): FileRepository {
        return object : FileRepository {

            private var job: Job? = null

            override fun downloadTrack(
                trackName: String,
                audioUrl: String,
                completion: (Response<Uri>) -> Unit
            ) {
                job?.cancel()
                job = GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val response = service.downloadTrack(audioUrl)
                        if (response.isSuccessful) {
                            val url = response.body()?.let {
                                downloadFile(trackName, it)
                            }
                            withContext(Dispatchers.Main) {
                                completion(Response.success(url))
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                completion(Response.error(response.code(), response.errorBody()))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(FILE_REPOSITORY, "$e")
                    }
                }
            }

            private fun downloadFile(trackName: String, response: ResponseBody): Uri {
                val file = createFile(trackName)
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

            private fun createFile(trackName: String): File {
                val dir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                    LOCAL_DIRECTORY
                )
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                return File(dir, trackName)
            }

        }
    }

    @Singleton
    @Provides
    fun provideMusicRepo(): MusicRepository {
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
        }
    }

    @Singleton
    @Provides
    fun provideTrackRepo(
        app: Application,
        service: YandexService,
        mappers: MusicMapper,
        db: AppDatabase,
        dao: TracksDao
    ): TrackRepository {
        return object : TrackRepository {

            private var job: Job? = null

            override fun fetchTrack(
                id: Int,
                isCaching: Boolean,
                completion: (ApiResponse<DomainTrack>) -> Unit
            ) {
                job?.cancel()
                job = GlobalScope.launch(Dispatchers.IO) {
                    val response = service.fetchTrack(id)
                    if (response.isSuccessful && response.body() != null) {

                        val track = mappers.trackToDomain(response.body()?.track!!)
                        if (isCaching) {
                            cacheTrack(track)
                        }

                        withContext(Dispatchers.Main) {
                            completion(ApiResponse.success(track))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            completion(ApiResponse.error(response.message()))
                        }
                    }
                }
            }

            override fun checkTrackExistence(trackName: String): Boolean {
                val file = getTrackFile(trackName)
                return file.exists()
            }

            override fun fetchTrackUri(trackName: String): Uri {
                if (!checkTrackExistence(trackName)) return Uri.EMPTY
                val file = getTrackFile(trackName)
                return Uri.fromFile(file)
            }

            private fun getTrackFile(trackName: String): File {
                val dir = File(
                    app.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                    LOCAL_DIRECTORY
                )
                return File(dir, trackName)
            }

            private fun cacheTrack(track: DomainTrack) {
                db.runInTransaction {
                    dao.insertTrack(track)
                }
            }
        }
    }
}

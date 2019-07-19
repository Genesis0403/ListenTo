package com.epam.listento.di

import android.app.Application
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.epam.listento.api.ApiResponse
import com.epam.listento.api.YandexService
import com.epam.listento.api.model.*
import com.epam.listento.db.AppDatabase
import com.epam.listento.db.TracksDao
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
        mappers: MusicMapper,
        db: AppDatabase,
        tracksDao: TracksDao
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

            override fun cacheTrack(track: Track) {
                GlobalScope.launch(Dispatchers.IO) {
                    db.runInTransaction {
                        tracksDao.insertTrack(track)
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
                            completion(Response.success(url))
                        } else {
                            completion(Response.error(response.code(), response.errorBody()))
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
                val trackName = "${track.artist}-${track.title}.mp3"
                storageRepository.fetchStorage(track.storageDir) { response ->
                    if (response.isSuccessful) {
                        response.body()?.let {
                            val downloadUrl = audioRepository.fetchAudioUrl(it)

                            onDownloadResponse(trackName, downloadUrl) { result ->
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

            private fun onDownloadResponse(
                trackName: String,
                downloadUrl: String,
                completion: (ApiResponse<Uri>) -> Unit
            ) {
                fileRepository.downloadTrack(trackName, downloadUrl) { response ->
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
    fun provideTrackRepo(
        app: Application,
        service: YandexService,
        db: AppDatabase,
        dao: TracksDao
    ): TrackRepository {
        return object : TrackRepository {

            private var job: Job? = null

            override fun fetchTrack(
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
                val url = "https://${apiUrl.replace("%%", "700x700")}"
                return track.run {
                    NotificationTrack(
                        id,
                        storageDir ?: "",
                        title ?: "None",
                        artists?.first()?.name ?: "None",
                        durationMs ?: 0,
                        url
                    )
                }
            }

            override fun cacheTrack(track: Track) {
                GlobalScope.launch(Dispatchers.IO) {
                    db.runInTransaction {
                        dao.insertTrack(track)
                    }
                }
            }

            override fun checkTrackExistence(track: Track): Boolean {
                val file = getTrackFile(track)
                return file.exists()
            }

            override fun fetchTrackUri(track: Track): Uri {
                if (!checkTrackExistence(track)) return Uri.EMPTY
                val file = getTrackFile(track)
                return Uri.fromFile(file)
            }

            private fun getTrackFile(track: Track): File {
                val name = "${track.artist?.name}-${track.title}.mp3"
                val dir = File(
                    app.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                    LOCAL_DIRECTORY
                )
                return File(dir, name)
            }
        }
    }
}

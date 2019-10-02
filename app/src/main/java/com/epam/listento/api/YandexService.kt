package com.epam.listento.api

import com.epam.listento.api.model.ApiStorage
import com.epam.listento.api.model.TrackRequest
import com.epam.listento.api.model.TracksRequest
import com.epam.listento.utils.Json
import com.epam.listento.utils.Xml
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface YandexService {

    @Json
    @GET("music-search.jsx?type=track")
    suspend fun searchTracks(
        @Query("text") type: String
    ): Response<TracksRequest>

    @Xml
    @GET("http://storage.mds.yandex.net/download-info/{storageDir}/2")
    suspend fun fetchStorage(
        @Path("storageDir") storageDir: String
    ): Response<ApiStorage>

    @GET
    @Streaming
    suspend fun downloadTrack(@Url url: String): Response<ResponseBody>

    @Json
    @GET("https://music.yandex.ru/handlers/track.jsx")
    suspend fun fetchTrack(
        @Query("track") id: Int
    ): Response<TrackRequest>
}

package com.epam.listento.api

import com.epam.listento.api.model.ApiStorage
import com.epam.listento.api.model.SearchRequest
import com.epam.listento.utils.Json
import com.epam.listento.utils.Xml
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YandexService {

    @Json
    @GET("music-search.jsx")
    suspend fun search(
        @Query("text") text: String,
        @Query("type") type: String
    ): Response<SearchRequest>

    @Xml
    @GET("http://storage.mds.yandex.net/download-info/{storageDir}/2")
    suspend fun fetchStorage(
        @Path("storageDir") storageDir: String
    ): Response<ApiStorage>

    @GET("http://{host}/get-mp3/{s}/{ts}{path}")
    fun fetchTrack(
        host: String,
        s: String,
        ts: String,
        path: String
    )
}

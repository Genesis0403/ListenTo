package com.epam.listento.api.model

import com.epam.listento.api.TrackIdTypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

data class ApiTrack(
    @JsonAdapter(TrackIdTypeAdapter::class)
    @field:SerializedName("id") val id: Int,
    @field:SerializedName("storageDir") val storageDir: String?,
    @field:SerializedName("durationMs") val durationMs: Long?,
    @field:SerializedName("title") val title: String?,
    @field:SerializedName("albums") val albums: List<ApiAlbum>?,
    @field:SerializedName("artists") val artists: List<ApiArtist>?
)

package com.epam.listento.api.model

import com.google.gson.annotations.SerializedName

data class SearchRequest(
    @field:SerializedName("text") val text: String?,
    @field:SerializedName("tracks") val tracks: Items<ApiTrack>?,
    @field:SerializedName("albums") val albums: Items<ApiAlbum>?
)

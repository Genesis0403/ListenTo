package com.epam.listento.api.model

import com.google.gson.annotations.SerializedName

data class ApiAlbum(
    @field:SerializedName("id") val id: Int?,
    @field:SerializedName("coverUri") val coverUri: String?,
    @field:SerializedName("storageDir") val storageDir: String?,
    @field:SerializedName("title") val title: String?,
    @field:SerializedName("artists") val artists: List<ApiArtist>?
)
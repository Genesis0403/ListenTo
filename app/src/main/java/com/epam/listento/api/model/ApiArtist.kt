package com.epam.listento.api.model

import com.google.gson.annotations.SerializedName

data class ApiArtist(
    @field:SerializedName("id") val id: Int,
    @field:SerializedName("uri") val uri: String?,
    @field:SerializedName("name") val name: String?,
    @field:SerializedName("cover") val cover: ApiCover
)

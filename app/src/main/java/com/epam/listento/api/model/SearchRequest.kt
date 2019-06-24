package com.epam.listento.api.model

import com.google.gson.annotations.SerializedName

data class SearchRequest(
    @field:SerializedName("text") val text: String?,
    @field:SerializedName("tracks") val tracks: Tracks?,
    @field:SerializedName("albums") val albums: Albums?
)
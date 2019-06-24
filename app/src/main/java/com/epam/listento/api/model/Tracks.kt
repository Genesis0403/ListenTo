package com.epam.listento.api.model

import com.google.gson.annotations.SerializedName

data class Tracks(
    @field:SerializedName("items") val items: List<ApiTrack>?
)
package com.epam.listento.api.model

import com.google.gson.annotations.SerializedName

class TracksRequest(
    @field:SerializedName("tracks") val tracks: Items<ApiTrack>?
)

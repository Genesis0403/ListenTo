package com.epam.listento.api.model

import com.google.gson.annotations.SerializedName

class TrackRequest(
    @field:SerializedName("track") val track: ApiTrack?
)
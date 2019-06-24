package com.epam.listento.api.model

import com.google.gson.annotations.SerializedName

data class Albums(
    @field:SerializedName("albums") val items: List<ApiAlbum>
)
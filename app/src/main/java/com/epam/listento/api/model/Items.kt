package com.epam.listento.api.model

import com.google.gson.annotations.SerializedName

class Items<T>(
    @field:SerializedName("items") val items: List<T>?
)

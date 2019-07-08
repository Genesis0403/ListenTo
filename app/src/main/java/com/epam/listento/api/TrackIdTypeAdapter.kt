package com.epam.listento.api

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter

class TrackIdTypeAdapter : TypeAdapter<Int>() {

    override fun write(out: JsonWriter?, value: Int?) {
        out?.value(value)
    }

    override fun read(reader: JsonReader?): Int {
        return reader?.nextInt() ?: 0
    }
}

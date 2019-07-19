package com.epam.listento.db

import androidx.room.TypeConverter
import com.epam.listento.model.Album
import com.epam.listento.model.Artist
import com.google.gson.Gson

class Converters {

    @TypeConverter
    fun fromArtist(artist: Artist): String {
        return Gson().toJson(artist)
    }

    @TypeConverter
    fun toArtist(json: String): Artist {
        return Gson().fromJson(json, Artist::class.java)
    }

    @TypeConverter
    fun fromAlbum(album: Album): String {
        return Gson().toJson(album)
    }

    @TypeConverter
    fun toAlbumt(json: String): Album {
        return Gson().fromJson(json, Album::class.java)
    }
}

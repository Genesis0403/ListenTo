package com.epam.listento.db

import androidx.room.TypeConverter
import com.epam.listento.domain.DomainAlbum
import com.epam.listento.domain.DomainArtist
import com.google.gson.Gson

class Converters {

    @TypeConverter
    fun fromArtist(artist: DomainArtist): String {
        return Gson().toJson(artist)
    }

    @TypeConverter
    fun toArtist(json: String): DomainArtist {
        return Gson().fromJson(json, DomainArtist::class.java)
    }

    @TypeConverter
    fun fromAlbum(album: DomainAlbum): String {
        return Gson().toJson(album)
    }

    @TypeConverter
    fun toAlbum(json: String): DomainAlbum {
        return Gson().fromJson(json, DomainAlbum::class.java)
    }

    @TypeConverter
    fun fromListArtist(artists: List<DomainArtist>): String {
        return Gson().toJson(artists)
    }

    @TypeConverter
    fun toListArtist(json: String): List<DomainArtist> {
        return (Gson().fromJson(json, Array<DomainArtist>::class.java)).toList()
    }

    @TypeConverter
    fun fromListAlbums(albums: List<DomainAlbum>): String {
        return Gson().toJson(albums)
    }

    @TypeConverter
    fun toListAlbums(json: String): List<DomainAlbum> {
        return (Gson().fromJson(json, Array<DomainAlbum>::class.java)).toList()
    }
}

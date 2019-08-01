package com.epam.listento.db

import androidx.room.TypeConverter
import com.epam.listento.domain.DomainAlbum
import com.epam.listento.domain.DomainArtist
import com.google.gson.Gson
import java.util.*

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromArtist(artist: DomainArtist): String {
        return gson.toJson(artist)
    }

    @TypeConverter
    fun toArtist(json: String): DomainArtist {
        return gson.fromJson(json, DomainArtist::class.java)
    }

    @TypeConverter
    fun fromAlbum(album: DomainAlbum): String {
        return gson.toJson(album)
    }

    @TypeConverter
    fun toAlbum(json: String): DomainAlbum {
        return gson.fromJson(json, DomainAlbum::class.java)
    }

    @TypeConverter
    fun fromListArtist(artists: List<DomainArtist>): String {
        return gson.toJson(artists)
    }

    @TypeConverter
    fun toListArtist(json: String): List<DomainArtist> {
        return (gson.fromJson(json, Array<DomainArtist>::class.java)).toList()
    }

    @TypeConverter
    fun fromListAlbums(albums: List<DomainAlbum>): String {
        return gson.toJson(albums)
    }

    @TypeConverter
    fun toListAlbums(json: String): List<DomainAlbum> {
        return (gson.fromJson(json, Array<DomainAlbum>::class.java)).toList()
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromDate(ms: Long?): Date? {
        return if (ms == null) null else Date(ms)
    }
}

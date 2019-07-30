package com.epam.listento.repository

import android.support.v4.media.MediaMetadataCompat
import com.epam.listento.model.player.utils.id
import com.epam.listento.repository.global.MusicRepository

class MusicRepositoryImpl : MusicRepository {

    private val tracks = mutableListOf<MediaMetadataCompat>()
    private var currentIndex = 0

    override fun isDataChanged(data: List<MediaMetadataCompat>): Boolean {
        return !tracks.containsAll(data)
    }

    override fun setCurrent(track: MediaMetadataCompat) {
        val index = tracks.indexOfFirst { it.id == track.id }
        if (index != -1) {
            currentIndex = index
        }
    }

    override fun getCurrent(): MediaMetadataCompat {
        return tracks[currentIndex]
    }

    override fun containsTrack(track: MediaMetadataCompat): Boolean {
        return tracks.contains(track)
    }

    override fun getNext(): MediaMetadataCompat {
        currentIndex = ++currentIndex % tracks.size
        return tracks[currentIndex]
    }

    override fun getPrevious(): MediaMetadataCompat {
        val previous = --currentIndex
        return if (previous > -1) {
            tracks[previous]
        } else {
            currentIndex = tracks.size - 1
            tracks[currentIndex]
        }
    }

    override fun setSource(data: List<MediaMetadataCompat>) {
        with(tracks) {
            clear()
            addAll(data)
            currentIndex = 0
        }
    }
}

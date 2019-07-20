package com.epam.listento.repository

import com.epam.listento.model.Track

interface MusicRepository {
    fun isDataChanged(data: List<Track>): Boolean
    fun getCurrent(): Track
    fun getNext(): Track
    fun getPrevious(): Track
    fun setSource(data: List<Track>)
    fun setCurrent(track: Track)
    fun containsTrack(track: Track): Boolean
}

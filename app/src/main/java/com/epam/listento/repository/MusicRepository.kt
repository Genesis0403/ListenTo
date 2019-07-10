package com.epam.listento.repository

import android.net.Uri
import com.epam.listento.api.ApiResponse
import com.epam.listento.model.NotificationTrack
import com.epam.listento.model.Track

interface MusicRepository {
    fun isDataChanged(data: List<Track>): Boolean
    fun getCurrent(): Track
    fun getNext(): Track
    fun getPrevious(): Track
    fun setSource(data: List<Track>)
    fun downloadTrack(track: NotificationTrack, completion: (ApiResponse<Uri>) -> Unit)
    fun setCurrent(track: Track)
    fun containsTrack(track: Track): Boolean
}

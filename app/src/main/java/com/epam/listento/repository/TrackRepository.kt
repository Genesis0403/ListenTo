package com.epam.listento.repository

import android.net.Uri
import com.epam.listento.api.ApiResponse
import com.epam.listento.model.NotificationTrack
import com.epam.listento.model.Track

interface TrackRepository {
    fun fetchTrack(id: Int, completion: (ApiResponse<NotificationTrack>) -> Unit)
    fun checkTrackExistence(track: Track): Boolean
    fun fetchTrackUri(track: Track): Uri
    fun cacheTrack(track: Track)
}

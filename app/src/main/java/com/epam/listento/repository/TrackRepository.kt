package com.epam.listento.repository

import com.epam.listento.api.ApiResponse
import com.epam.listento.model.NotificationTrack

interface TrackRepository {
    fun fecthTrack(id: Int, completion: (ApiResponse<NotificationTrack>) -> Unit)
}

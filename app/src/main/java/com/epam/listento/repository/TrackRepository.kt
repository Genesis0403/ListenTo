package com.epam.listento.repository

import android.net.Uri
import com.epam.listento.api.ApiResponse
import com.epam.listento.domain.DomainTrack
import com.epam.listento.model.NotificationTrack
import com.epam.listento.model.Track

interface TrackRepository {
    fun fetchTrack(id: Int, isCaching: Boolean, completion: (ApiResponse<DomainTrack>) -> Unit)
    fun checkTrackExistence(trackName: String): Boolean
    fun fetchTrackUri(trackName: String): Uri
}

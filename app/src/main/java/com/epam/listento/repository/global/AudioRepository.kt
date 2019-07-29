package com.epam.listento.repository.global

import com.epam.listento.domain.DomainStorage

interface AudioRepository {
    fun fetchAudioUrl(storage: DomainStorage): String
}

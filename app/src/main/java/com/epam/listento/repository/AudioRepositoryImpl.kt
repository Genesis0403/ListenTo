package com.epam.listento.repository

import com.epam.listento.domain.DomainStorage
import com.epam.listento.repository.global.AudioRepository
import javax.inject.Inject

class AudioRepositoryImpl @Inject constructor() : AudioRepository {
    override fun fetchAudioUrl(storage: DomainStorage): String {
        return StringBuilder("https://").apply {
            append(storage.host)
            append("/get-mp3/")
            append(storage.s)
            append("/")
            append(storage.ts)
            append(storage.path)
        }.toString()
    }
}

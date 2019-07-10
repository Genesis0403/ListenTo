package com.epam.listento.model

import java.lang.StringBuilder
import java.util.concurrent.TimeUnit

object MsMapper {

    fun convert(duration: Int): String {
        val timing = duration.toLong()
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timing) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timing) % 60
        return StringBuilder().apply {
            append(minutes)
            append(":")
            if (seconds < 10) {
                append("0$seconds")
            } else {
                append(seconds)
            }
        }.toString()
    }
}

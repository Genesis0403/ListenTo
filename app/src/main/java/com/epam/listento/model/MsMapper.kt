package com.epam.listento.model

import java.util.concurrent.TimeUnit

object MsMapper {
    fun convert(duration: Int): String {
        val timing = duration.toLong()
        return String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(timing) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(timing) % TimeUnit.MINUTES.toSeconds(1)
        )
    }
}

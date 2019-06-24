package com.epam.listento.api.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "download-info", strict = false)
data class ApiStorage @JvmOverloads constructor(
    @field:Element(name = "host") val host: String = "",
    @field:Element(name = "path") val path: String = "",
    @field:Element(name = "ts") val ts: String = "",
    @field:Element(name = "s") val s: String = ""
)

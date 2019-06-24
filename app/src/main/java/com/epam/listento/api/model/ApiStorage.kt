package com.epam.listento.api.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "download-info", strict = false)
data class ApiStorage @JvmOverloads constructor(
    @field:Element(name = "host") var host: String = "",
    @field:Element(name = "path") var path: String = "",
    @field:Element(name = "ts") var ts: String = "",
    @field:Element(name = "s") var s: String = ""
)

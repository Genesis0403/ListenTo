package com.epam.listento.utils

import android.content.Context

interface ContextProvider {
    fun context(): Context
    fun getString(id: Int): String
}

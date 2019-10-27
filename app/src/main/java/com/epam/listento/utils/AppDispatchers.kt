package com.epam.listento.utils

import kotlinx.coroutines.CoroutineDispatcher

interface AppDispatchers {
    val ui: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
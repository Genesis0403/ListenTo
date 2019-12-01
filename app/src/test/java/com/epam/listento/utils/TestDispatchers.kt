package com.epam.listento.utils

import kotlinx.coroutines.Dispatchers

class TestDispatchers : AppDispatchers {
    override val ui = Dispatchers.Unconfined
    override val io = Dispatchers.Unconfined
    override val default = Dispatchers.Unconfined
}
package com.epam.listento.utils

import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*

internal class DebounceSearchListener(
    lifecycle: Lifecycle,
    private val onResult: (query: String) -> Unit
) : SearchView.OnQueryTextListener, LifecycleObserver {

    private companion object {
        private const val DELAY: Long = 1000
    }

    init {
        lifecycle.addObserver(this)
    }

    private var job: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        return if (newText != null) {
            job?.cancel()
            job = coroutineScope.launch {
                delay(DELAY)
                onResult(newText)
            }
            true
        } else {
            false
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        job?.cancel()
    }

    //TODO read about jobs pausing while lifecycle component paused too
}

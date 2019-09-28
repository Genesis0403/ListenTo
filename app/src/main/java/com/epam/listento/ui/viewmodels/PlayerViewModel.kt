package com.epam.listento.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Timer
import javax.inject.Inject
import javax.inject.Provider
import kotlin.concurrent.schedule

private const val SECOND: Long = 1000
private const val DELAY: Long = 0

class PlayerViewModel @Inject constructor() : ViewModel() {

    private var timer: Timer? = null
    private var job: Job? = null

    fun startScheduler(action: () -> Unit) {
        timer?.let { return }
        timer = Timer().also {
            it.schedule(DELAY, SECOND) {
                job = CoroutineScope(Dispatchers.Main).launch {
                    action()
                }
            }.run()
        }
    }

    fun stopScheduler() {
        job?.cancel()
        timer?.cancel()
        timer = null
    }

    class Factory @Inject constructor(
        private val provider: Provider<PlayerViewModel>
    ) : ViewModelProvider.Factory {

        @Suppress("UNCKECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return provider.get() as T
        }
    }
}

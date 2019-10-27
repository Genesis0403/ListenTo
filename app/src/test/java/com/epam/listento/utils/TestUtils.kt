package com.epam.listento.utils

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import org.spekframework.spek2.dsl.Root

fun Root.emulateInstanteTaskExecutorRule() {

    beforeEachTest {
        ArchTaskExecutor.getInstance().setDelegate(TaskExecutorImpl())
    }

    afterEachTest {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
}

class TaskExecutorImpl : TaskExecutor() {
    override fun executeOnDiskIO(runnable: Runnable) {
        runnable.run()
    }

    override fun isMainThread(): Boolean = true

    override fun postToMainThread(runnable: Runnable) {
        runnable.run()
    }
}
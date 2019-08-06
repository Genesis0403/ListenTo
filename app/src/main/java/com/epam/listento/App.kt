package com.epam.listento

import android.app.Application
import com.epam.listento.di.AppComponent
import com.epam.listento.di.DaggerAppComponent

class App : Application() {

    companion object {
        private const val TAG = "APPLICATION"

        private lateinit var _component: AppComponent
        val component: AppComponent get() = _component
    }

    override fun onCreate() {
        super.onCreate()
        _component = DaggerAppComponent.builder()
            .application(this)
            .build()
    }
    // TODO stop service if not stopped
}

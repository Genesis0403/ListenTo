package com.epam.listento

import android.app.Application
import com.epam.listento.di.AppComponent
import com.epam.listento.di.DaggerAppComponent

// TODO download bitmap with Glide get() function
// TODO implement MusicSource where you will download tracks and pass them into service
// TODO implement preparation of Session via ExoPlayer (onPrepareFromMediaId)

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
}

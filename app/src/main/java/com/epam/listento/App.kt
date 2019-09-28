package com.epam.listento

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.epam.listento.di.AppComponent
import com.epam.listento.di.DaggerAppComponent

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        _component = DaggerAppComponent.builder()
            .application(this)
            .build()

        setUiMode()
    }

    override fun onTerminate() {
        super.onTerminate()
        // TODO stop service if not stopped
    }

    private fun setUiMode() {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val isNightMode = sp.getBoolean(getString(R.string.night_mode_key), false)
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    companion object {
        private lateinit var _component: AppComponent
        val component: AppComponent get() = _component
    }
}

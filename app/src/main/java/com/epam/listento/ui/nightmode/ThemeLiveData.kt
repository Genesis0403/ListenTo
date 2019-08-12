package com.epam.listento.ui.nightmode

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.LiveData
import com.epam.listento.R

class ThemeLiveData(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) : LiveData<Int>() {

    private val listener = SharedPreferenceListener()

    override fun onActive() {
        super.onActive()
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onInactive() {
        super.onInactive()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun getUiMode(isNight: Boolean): Int {
        return if (isNight) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    private inner class SharedPreferenceListener : SharedPreferences.OnSharedPreferenceChangeListener {

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == context.getString(R.string.night_mode_key)) {
                val isNight = sharedPreferences?.getBoolean(key, false) ?: false
                postValue(getUiMode(isNight))
            }
        }
    }
}
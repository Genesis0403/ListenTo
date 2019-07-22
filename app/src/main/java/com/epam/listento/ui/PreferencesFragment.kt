package com.epam.listento.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.epam.listento.R


class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}

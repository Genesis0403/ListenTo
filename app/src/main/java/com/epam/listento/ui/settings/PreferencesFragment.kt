package com.epam.listento.ui.settings

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.epam.listento.App
import com.epam.listento.R
import javax.inject.Inject

class PreferencesFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var factory: SettingsViewModel.Factory
    private val mainViewModel: SettingsViewModel by viewModels {
        factory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)

        activity?.findViewById<Toolbar>(R.id.appToolBar)?.apply {
            menu.clear()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val preference = findPreference<Preference>(CLEAR_CACHE)
        preference?.setOnPreferenceClickListener {
            mainViewModel.clearCache()
            true
        }
    }

    private companion object {
        private const val CLEAR_CACHE = "clear_cache"
    }
}

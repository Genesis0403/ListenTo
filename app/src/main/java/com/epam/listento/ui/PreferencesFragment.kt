package com.epam.listento.ui

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.ui.viewmodels.MainViewModel
import javax.inject.Inject

private const val CLEAR_CACHE = "clear_cache"

class PreferencesFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var factory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by activityViewModels {
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
}

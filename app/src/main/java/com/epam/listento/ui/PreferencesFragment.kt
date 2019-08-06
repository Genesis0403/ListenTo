package com.epam.listento.ui

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.ui.viewmodels.MainViewModel
import javax.inject.Inject

private const val CLEAR_CACHE = "clear_cache"

class PreferencesFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        App.component.inject(this)
        super.onCreate(savedInstanceState)

        activity?.findViewById<Toolbar>(R.id.appToolBar)?.apply {
            menu.clear()
        }

        mainViewModel = ViewModelProviders.of(requireActivity(), factory)[MainViewModel::class.java]
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

package com.epam.listento.ui

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.ui.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.activity_main.appToolBar
import kotlinx.android.synthetic.main.activity_main.navigationBar
import javax.inject.Inject

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    @Inject
    lateinit var factory: MainViewModel.Factory
    private val mainViewModel: MainViewModel by viewModels {
        factory
    }

    private val navController by lazy { findNavController(R.id.navHostFragment) }

    private val appBarConfiguration by lazy {
        AppBarConfiguration(
            setOf(
                R.id.searchFragment,
                R.id.cacheFragment,
                R.id.preferencesFragment
            )
        )
    }

    private val sp: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)

        sp.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)

        appToolBar.setupWithNavController(navController, appBarConfiguration)
        navigationBar.setupWithNavController(navController)

        mainViewModel.nightMode.observe(this, Observer<Int> { uiMode ->
            if (uiMode != AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.setDefaultNightMode(uiMode)
                recreate()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        sp.unregisterOnSharedPreferenceChangeListener(sharedPreferencesListener)
    }

    private val sharedPreferencesListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            val isNightMode = sp.getBoolean(key, false)
            mainViewModel.handleThemeChange(isNightMode, key)
        }
}

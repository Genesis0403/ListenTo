package com.epam.listento.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.ui.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

private const val TAG = "MAIN_ACTIVITY"

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private lateinit var mainViewModel: MainViewModel

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

    override fun onCreate(savedInstanceState: Bundle?) {
        App.component.inject(this)
        mainViewModel = ViewModelProviders.of(this, factory)[MainViewModel::class.java]

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainViewModel.nightMode.observe(this, Observer<Int> { uiMode ->
            if (uiMode != AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.setDefaultNightMode(uiMode)
                recreate()
            }
        })

        appToolBar.setupWithNavController(navController, appBarConfiguration)
        navigationBar.setupWithNavController(navController)
    }
}

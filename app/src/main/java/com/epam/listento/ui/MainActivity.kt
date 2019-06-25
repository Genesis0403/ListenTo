package com.epam.listento.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.epam.listento.App
import com.epam.listento.R
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

private const val TAG = "MAIN_ACTIVITY"

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private lateinit var mainViewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
        setContentView(R.layout.activity_main)
        mainViewModel = ViewModelProviders.of(this, factory)[MainViewModel::class.java]

        navigationBar.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.search -> {
                    if (navigationBar.selectedItemId != R.id.search) {
                        loadFragment(R.id.contentContainer, TracksFragment.newInstance())
                    }
                }
                R.id.player -> {
                }
                R.id.settings -> {
                }
                else -> return@setOnNavigationItemSelectedListener false
            }
            true
        }
    }

    private fun loadFragment(id: Int, fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(id, fragment)
            .commit()
    }
}


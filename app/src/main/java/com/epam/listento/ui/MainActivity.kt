package com.epam.listento.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
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

    private val fragmentsAdapter = BottomNavigationAdapter(
        mapOf(
            R.id.searchFragment to SearchFragment.newInstance(),
            R.id.playerFragment to PlayerFragment.newInstance(),
            R.id.cacheFragment to PlaylistFragment.newInstance()
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
        setContentView(R.layout.activity_main)
        mainViewModel = ViewModelProviders.of(this, factory)[MainViewModel::class.java]

        savedInstanceState ?: supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, fragmentsAdapter[R.id.searchFragment])
            .commit()

        navigationBar.setOnNavigationItemSelectedListener {
            moveToFragment(it)
        }
    }

    private fun moveToFragment(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.searchFragment -> {
                loadFragment(R.id.contentContainer, R.id.searchFragment)
            }
            R.id.playerFragment -> {
                loadFragment(R.id.contentContainer, R.id.playerFragment)
            }
            R.id.cacheFragment -> {
                loadFragment(R.id.contentContainer, R.id.cacheFragment)
            }
            R.id.settings -> {
                //TODO implement preferences
            }
            else -> return false
        }
        return true
    }

    private fun loadFragment(containerId: Int, fragmentId: Int) {
        if (navigationBar.selectedItemId != fragmentId) {
            supportFragmentManager.beginTransaction()
                .replace(containerId, fragmentsAdapter[fragmentId])
                .commit()
        }
    }
}

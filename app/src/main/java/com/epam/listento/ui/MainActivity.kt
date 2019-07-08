package com.epam.listento.ui

import android.os.Bundle
import android.view.MenuItem
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

    private val fragmentsAdapter = BottomNavigationAdapter(
        mapOf(
            R.id.searchFragment to SearchFragment.newInstance(),
            R.id.playerFrament to PlayerFragment.newInstance()
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
        setContentView(R.layout.activity_main)
        mainViewModel = ViewModelProviders.of(this, factory)[MainViewModel::class.java]

        savedInstanceState ?: loadFragment(R.id.contentContainer, fragmentsAdapter[R.id.searchFragment])

        navigationBar.setOnNavigationItemSelectedListener {
            moveToFragment(it)
        }
    }

    private fun moveToFragment(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.searchFragment -> {
                if (navigationBar.selectedItemId != R.id.searchFragment) {
                    loadFragment(R.id.contentContainer, fragmentsAdapter[R.id.searchFragment])
                }
            }
            R.id.playerFrament -> {
                if (navigationBar.selectedItemId != R.id.playerFrament) {
                    loadFragment(R.id.contentContainer, fragmentsAdapter[R.id.playerFrament])
                }
            }
            R.id.settings -> {
            }
            else -> return false
        }
        return true
    }

    private fun loadFragment(id: Int, fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(id, fragment)
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        // stopService(Intent(this, PlayerService::class.java))
    }
}

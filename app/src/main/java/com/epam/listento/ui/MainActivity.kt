package com.epam.listento.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.utils.DebounceSearchListener
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

        val navController = findNavController(R.id.navHostFragment)

        listenToSearchViewQuery(querySearchView)

        navigationBar.setupWithNavController(navController)
    }

    private fun listenToSearchViewQuery(searchView: SearchView) {
        searchView.setOnQueryTextListener(DebounceSearchListener(this.lifecycle) { query ->
            if (query.isNotEmpty()) {
                mainViewModel.lastQuery.value = query
            }
        })
    }
}
package com.epam.listento.ui.search

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.api.ApiResponse
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.ui.TracksAdapter
import com.epam.listento.ui.dialogs.TrackDialogDirections
import com.epam.listento.utils.DebounceSearchListener
import kotlinx.android.synthetic.main.tracks_fragment.progressBar
import kotlinx.android.synthetic.main.tracks_fragment.tracksRecyclerView
import javax.inject.Inject

class SearchFragment :
    Fragment(R.layout.tracks_fragment),
    TracksAdapter.OnClickListener {

    private val searchScreenViewModel: SearchScreenViewModel by activityViewModels {
        searchFactory
    }

    @Inject
    lateinit var searchFactory: SearchScreenViewModel.Factory

    private val navController by lazy { findNavController() }

    private val tracksAdapter = TracksAdapter(this)

    override fun onClick(track: Track) {
        searchScreenViewModel.handleItemClick(track)
    }

    override fun onLongClick(track: Track) {
        searchScreenViewModel.handleLongItemClick(track)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().findViewById<Toolbar>(R.id.appToolBar).apply {
            menu.clear()
            inflateMenu(R.menu.search_toolbar_menu)
            val searchView = menu.findItem(R.id.actionSearch).actionView as SearchView
            listenToSearchViewQuery(searchView)
        }

        tracksRecyclerView.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = tracksAdapter
        }

        initObservers()
    }

    override fun onMenuClick(track: Track) {
    }

    private fun listenToSearchViewQuery(searchView: SearchView) {
        searchView.setOnQueryTextListener(DebounceSearchListener(this.lifecycle) { query ->
            progressBar.isVisible = true
            searchScreenViewModel.fetchTracks(query)
        })
    }

    private fun initObservers() {
        with(searchScreenViewModel) {
            tracks.observe(viewLifecycleOwner, Observer<ApiResponse<List<Track>>> {
                if (it.status.isSuccess()) {
                    val newData = it.body ?: emptyList()
                    tracksAdapter.submitList(newData)
                } else {
                    Toast.makeText(
                        context,
                        getString(R.string.failed_tracks_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                progressBar.isVisible = false
            })

            serviceHelper.currentPlaying.observe(viewLifecycleOwner, Observer<Int> {
                handlePlayerStateChange(it)
            })

            serviceHelper.playbackState.observe(
                viewLifecycleOwner,
                Observer<PlaybackState> {
                    handlePlayerStateChange(serviceHelper.currentPlaying.value ?: -1)
                }
            )

            command.observe(
                viewLifecycleOwner,
                Observer<SearchScreenViewModel.Command> { action ->
                    when (action) {
                        SearchScreenViewModel.Command.StopLoading ->
                            progressBar.isVisible = false
                        SearchScreenViewModel.Command.ShowPlayerActivity ->
                            navController.navigate(R.id.playerActivity)
                        SearchScreenViewModel.Command.PlayTrack ->
                            serviceHelper.transportControls?.play()
                        is SearchScreenViewModel.Command.ShowCacheDialog -> {
                            val actionId = TrackDialogDirections.actionTrackDialog(
                                action.id,
                                action.title,
                                action.artist
                            )
                            navController.navigate(actionId)
                        }
                    }
                })
        }
    }

    companion object {
        private const val TAG = "SEARCH_FRAGMENT"
        private const val SEARCH_QUERY_MAX = 30
    }
}

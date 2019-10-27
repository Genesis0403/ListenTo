package com.epam.listento.ui.search

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.epam.listento.model.PlayerService
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.player.utils.id
import com.epam.listento.ui.TracksAdapter
import com.epam.listento.ui.dialogs.TrackDialogDirections
import com.epam.listento.utils.DebounceSearchListener
import kotlinx.android.synthetic.main.tracks_fragment.progressBar
import kotlinx.android.synthetic.main.tracks_fragment.tracksRecyclerView
import javax.inject.Inject

class SearchFragment : Fragment(), TracksAdapter.OnClickListener {

    @Inject
    lateinit var searchFactory: SearchScreenViewModel.Factory

    private val searchScreenViewModel: SearchScreenViewModel by activityViewModels {
        searchFactory
    }

    private val navController by lazy { findNavController() }

    private val tracksAdapter = TracksAdapter(this)

    private var binder: PlayerService.PlayerBinder? = null
    private var controller: MediaControllerCompat? = null

    override fun onClick(track: Track) {
        binder?.let {
            searchScreenViewModel.handleItemClick(track)
        }
    }

    override fun onLongClick(track: Track) {
        searchScreenViewModel.handleLongItemClick(track)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
    }

    override fun onStart() {
        super.onStart()
        activity?.bindService(
            Intent(activity, PlayerService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.tracks_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().findViewById<Toolbar>(R.id.appToolBar)?.apply {
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

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "DESTROYED")
        binder = null
        controller = null
        activity?.unbindService(connection)
    }

    private fun listenToSearchViewQuery(searchView: SearchView) {
        searchView.setOnQueryTextListener(DebounceSearchListener(this.lifecycle) { query ->
            if (query.isNotEmpty()) {
                progressBar.isVisible = true
                searchScreenViewModel.fetchTracks(query)
            }
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

            currentPlaying.observe(viewLifecycleOwner, Observer<Track> {
                searchScreenViewModel.handlePlayerStateChange(it.id)
            })

            command.observe(
                viewLifecycleOwner,
                Observer<SearchScreenViewModel.Command> { action ->
                    when (action) {
                        SearchScreenViewModel.Command.ShowPlayerActivity -> {
                            navController.navigate(R.id.playerActivity)
                        }
                        is SearchScreenViewModel.Command.PlayTrack -> {
                            controller?.transportControls?.play()
                        }
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

            playbackState.observe(
                viewLifecycleOwner,
                Observer<PlaybackState> {
                    handlePlayerStateChange(currentPlaying.value?.id ?: -1)
                }
            )
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            controller = null
            binder = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as PlayerService.PlayerBinder
            val token = binder?.getSessionToken() ?: return
            try {
                controller = MediaControllerCompat(requireActivity(), token).also {
                    it.registerCallback(controllerCallback)
                }
            } catch (e: Exception) {
                controller = null
            }
        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            searchScreenViewModel.handlePlaybackStateChange(
                state?.state ?: PlaybackStateCompat.STATE_NONE
            )
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            val id = metadata?.id?.toInt() ?: -1
            searchScreenViewModel.handleMetadataChange(id)
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            searchScreenViewModel.handlePlayerStateChange()
            controller?.unregisterCallback(this)
        }
    }

    companion object {
        private const val TAG = "SEARCH_FRAGMENT"
    }
}

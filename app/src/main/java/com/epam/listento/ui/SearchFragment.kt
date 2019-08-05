package com.epam.listento.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.api.ApiResponse
import com.epam.listento.model.PlayerService
import com.epam.listento.model.Track
import com.epam.listento.model.player.MediaSessionManager
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.player.utils.id
import com.epam.listento.ui.viewmodels.MainViewModel
import com.epam.listento.utils.DebounceSearchListener
import kotlinx.android.synthetic.main.tracks_fragment.*
import javax.inject.Inject

class SearchFragment : Fragment(), TracksAdapter.OnClickListener {

    companion object {
        private const val TAG = "SEARCH_FRAGMENT"
    }

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private lateinit var mainViewModel: MainViewModel

    private lateinit var sessionManager: MediaSessionManager
    private val tracksAdapter = TracksAdapter(this)

    private var binder: PlayerService.PlayerBinder? = null
    private var controller: MediaControllerCompat? = null

    override fun onClick(track: Track) {
        binder?.let {
            val current = sessionManager.currentPlaying.value
            val state = sessionManager.isPlaying.value
            if (state != PlaybackState.STOPPED
                && state != PlaybackState.PAUSED
                && current?.id == track.id.toString()
            ) {
                findNavController().navigate(R.id.playerActivity)
            } else {
                mainViewModel.tracks.value?.let {
                    mainViewModel.itemClick(track, it.body!!)
                    sessionManager.transportControls.play()
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        App.component.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProviders.of(requireActivity(), factory)[MainViewModel::class.java]
    }

    override fun onStart() {
        super.onStart()
        activity?.bindService(Intent(activity, PlayerService::class.java), connection, Context.BIND_AUTO_CREATE)
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
        val recycler = view.findViewById<RecyclerView>(R.id.tracksRecyclerView)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        activity?.findViewById<Toolbar>(R.id.appToolBar)?.apply {
            menu.clear()
            inflateMenu(R.menu.search_toolbar_menu)
            val searchView = menu.findItem(R.id.actionSearch).actionView as SearchView
            listenToSearchViewQuery(searchView, progress)
        }

        recycler.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = tracksAdapter
        }

        mainViewModel.tracks.observe(this, Observer<ApiResponse<List<Track>>> { response ->
            observeTrackList(response)
        })

        mainViewModel.lastQuery.observe(this, Observer<String> { query ->
            progress.visibility = ProgressBar.VISIBLE
            mainViewModel.fetchTracks(query)
        })
    }

    private fun listenToSearchViewQuery(searchView: SearchView, progress: ProgressBar) {
        searchView.setOnQueryTextListener(DebounceSearchListener(this.lifecycle) { query ->
            if (query.isNotEmpty()) {
                progress.visibility = View.VISIBLE
                mainViewModel.fetchTracks(query)
            }
        })
    }

    private fun observeTrackList(response: ApiResponse<List<Track>>) {
        if (response.status.isSuccess()) {
            val newData = response.body ?: emptyList()
            tracksAdapter.submitList(newData)
            progress.visibility = View.GONE
        } else {
            Toast.makeText(context, getString(R.string.failed_tracks_toast), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "DESTROYED")
        binder = null
        controller = null
        activity?.unbindService(connection)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            controller = null
            binder = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as PlayerService.PlayerBinder
            binder?.let {
                val token = it.getSessionToken() ?: return
                val app = activity?.applicationContext ?: return
                sessionManager = MediaSessionManager.getInstance(app, token)
                try {
                    controller = MediaControllerCompat(requireActivity(), token)
                    observeCurrentPlaying()
                } catch (e: Exception) {
                    controller = null
                }
            }
        }
    }

    private fun observeCurrentPlaying() {
        sessionManager.currentPlaying.observe(this, Observer<MediaMetadataCompat> { item ->
            sessionManager.isPlaying.value?.let { isPlaying ->
                mainViewModel.searchPlaybackChange(item.id.toInt(), isPlaying)
            }
        })
    }
}

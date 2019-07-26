package com.epam.listento.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
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
import com.epam.listento.model.PlayerService
import com.epam.listento.model.Track
import com.epam.listento.model.player.MediaSessionManager
import kotlinx.android.synthetic.main.tracks_fragment.*
import javax.inject.Inject

class PlaylistFragment : Fragment(), TracksAdapter.OnClickListener {
    companion object {
        private const val TAG = "PLAYLIST_FRAGMENT"
        private const val RECYCLER_POSITION = "RECYCLER_POSITION"

        fun newInstance() = PlaylistFragment()
    }

    @Inject
    lateinit var factory: ViewModelProvider.Factory
    private lateinit var mainViewModel: MainViewModel

    private val tracksAdapter = TracksAdapter(this)
    lateinit var sessionManager: MediaSessionManager

    private var binder: PlayerService.PlayerBinder? = null
    private var controller: MediaControllerCompat? = null


    override fun onClick(track: Track) {
        binder?.let {
            val title = sessionManager.currentPlaying.value?.description?.title
            val artist = sessionManager.currentPlaying.value?.description?.subtitle
            if (track.artist?.name == artist && track.title == title) {
                findNavController().navigate(R.id.playerActivity)
            } else {
                it.changeSourceData(mainViewModel.cachedTracks.value ?: emptyList())
                it.playTrack(track)
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
        activity?.bindService(Intent(activity, PlayerService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.cache_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recycler = view.findViewById<RecyclerView>(R.id.tracksRecyclerView)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        activity?.findViewById<Toolbar>(R.id.appToolBar)?.apply {
            menu.clear()
            inflateMenu(R.menu.search_toolbar_menu)
        }

        recycler.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = tracksAdapter
        }

        mainViewModel.cachedTracks.observe(this, Observer<List<Track>> { tracks ->
            observeTrackList(tracks)
        })
    }

    private fun observeTrackList(tracks: List<Track>) {
        progress.visibility = ProgressBar.GONE
        tracksAdapter.setTracks(tracks)
    }

    override fun onDestroy() {
        super.onDestroy()
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
                } catch (e: Exception) {
                    controller = null
                }
            }
        }
    }
}

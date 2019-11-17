package com.epam.listento.ui.cache

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
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.model.PlayerService
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.player.utils.id
import com.epam.listento.ui.TracksAdapter
import com.epam.listento.ui.dialogs.AlbumCreationDialog
import com.epam.listento.ui.dialogs.TrackDialogDirections
import kotlinx.android.synthetic.main.tracks_fragment.progressBar
import kotlinx.android.synthetic.main.tracks_fragment.tracksRecyclerView
import javax.inject.Inject

class CacheFragment : Fragment(), TracksAdapter.OnClickListener {

    @Inject
    lateinit var cacheFactory: CacheScreenViewModel.Factory
    private val cacheViewModel: CacheScreenViewModel by activityViewModels {
        cacheFactory
    }

    private val navController by lazy { findNavController() }

    private val tracksAdapter = TracksAdapter(this)

    private var binder: PlayerService.PlayerBinder? = null
    private var controller: MediaControllerCompat? = null

    override fun onClick(track: Track) {
        binder?.let {
            cacheViewModel.handleItemClick(track)
        }
    }

    override fun onLongClick(track: Track) {
        AlbumCreationDialog.newInstance().show(requireActivity().supportFragmentManager, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
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

        activity?.findViewById<Toolbar>(R.id.appToolBar)?.apply {
            menu.clear()
            inflateMenu(R.menu.search_toolbar_menu)
        }

        tracksRecyclerView.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = tracksAdapter
        }

        initObservers()
    }

    override fun onStart() {
        super.onStart()
        activity?.bindService(
            Intent(activity, PlayerService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onPause() {
        super.onPause()
        binder = null
        controller = null
        activity?.unbindService(connection)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DESTROYED")
    }

    override fun onMenuClick(track: Track) {
        cacheViewModel.handleThreeDotButtonClick(track)
    }

    private fun initObservers() {
        with(cacheViewModel) {

            tracks.observe(viewLifecycleOwner, Observer<List<Track>> {
                val newData = it ?: emptyList()
                tracksAdapter.submitList(newData)
                progressBar.isVisible = false
            })

            currentPlaying.observe(viewLifecycleOwner, Observer<Track> {
                cacheViewModel.handlePlayerStateChange(it.id)
            })

            command.observe(
                viewLifecycleOwner,
                Observer<CacheScreenViewModel.Command> { action ->
                    when (action) {
                        CacheScreenViewModel.Command.ShowPlayerActivity -> {
                            navController.navigate(R.id.playerActivity)
                        }
                        is CacheScreenViewModel.Command.PlayTrack -> {
                            controller?.transportControls?.play()
                        }
                        is CacheScreenViewModel.Command.ShowCacheDialog -> {
                            val actionId =
                                TrackDialogDirections.actionTrackDialog(
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
            binder?.let { binder ->
                val token = binder.getSessionToken() ?: return
                try {
                    controller = MediaControllerCompat(requireActivity(), token).also {
                        it.registerCallback(callback)
                    }
                } catch (e: Exception) {
                    controller = null
                }
            }
        }
    }

    private val callback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            Log.d(TAG, "PLAYBACK")
            cacheViewModel.handlePlaybackStateChange(
                state?.state ?: PlaybackStateCompat.STATE_NONE
            )
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            Log.d(TAG, "METADATA")
            val id = metadata?.id?.toInt() ?: -1
            cacheViewModel.handleMetadataChange(id)
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            cacheViewModel.handlePlayerStateChange()
            controller?.unregisterCallback(this)
        }
    }

    companion object {
        private const val TAG = "CacheFragment"
    }
}

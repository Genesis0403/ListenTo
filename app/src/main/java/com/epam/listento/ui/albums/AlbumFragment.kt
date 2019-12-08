package com.epam.listento.ui.albums

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
import android.view.View
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.model.PlayerService
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.model.player.utils.id
import kotlinx.android.synthetic.main.album_fragment.albumCover
import kotlinx.android.synthetic.main.album_fragment.toolbar
import kotlinx.android.synthetic.main.album_fragment.tracksRecyclerView
import javax.inject.Inject

class AlbumFragment :
    Fragment(R.layout.album_fragment),
    AlbumTracksAdapter.OnClickListener {

    private val albumTitle get() = arguments?.getString(TITLE, "") ?: ""
    private val albumId get() = arguments?.getInt(ID, -1) ?: -1
    private val cover get() = arguments?.getString(COVER, "") ?: ""

    private val tracksAdapter = AlbumTracksAdapter(this)

    private val albumViewModel by viewModels<AlbumViewModel> {
        factory.create(
            albumTitle,
            albumId
        )
    }

    private var binder: PlayerService.PlayerBinder? = null
    private var controller: MediaControllerCompat? = null

    @Inject
    lateinit var factory: AlbumViewModelFactory.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
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

    override fun onClick(track: Track) {
        albumViewModel.handleClick(track)
    }

    private fun initViews() {
        toolbar.title = albumTitle
        toolbar.setNavigationOnClickListener {
            if (isAdded) requireActivity().finish()
        }

        tracksRecyclerView.adapter = tracksAdapter
        tracksRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        if (cover.isNotEmpty()) {
            loadImage(albumCover, cover)
        }
    }

    private fun initObservers() {
        with(albumViewModel) {
            tracks.observe(viewLifecycleOwner, Observer<List<Track>> {
                tracksAdapter.submitList(it)
            })

            command.observe(viewLifecycleOwner, Observer<AlbumViewModel.Command> {
                when (it) {
                    AlbumViewModel.Command.PlayTrack ->
                        controller?.transportControls?.play()
                    AlbumViewModel.Command.PauseTrack ->
                        controller?.transportControls?.pause()
                }
            })

            currentPlaying.observe(viewLifecycleOwner, Observer<Track> {
                albumViewModel.handlePlayerStateChange(it.id)
            })

            playbackState.observe(
                viewLifecycleOwner,
                Observer<PlaybackState> {
                    handlePlayerStateChange(currentPlaying.value?.id ?: -1)
                }
            )
        }
    }

    private fun loadImage(imageView: ImageView, url: String) {
        Glide.with(imageView)
            .load(url)
            .error(R.drawable.no_photo_24dp)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)
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
            albumViewModel.handlePlaybackStateChange(
                state?.state ?: PlaybackStateCompat.STATE_NONE
            )
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            Log.d(TAG, "METADATA")
            val id = metadata?.id?.toInt() ?: -1
            albumViewModel.handleMetadataChange(id)
        }

        override fun onSessionDestroyed() {
            super.onSessionDestroyed()
            albumViewModel.handlePlayerStateChange()
            controller?.unregisterCallback(this)
        }
    }

    companion object {
        const val TAG = "AlbumFragment"
        private const val TITLE = "TITLE"
        private const val ID = "ID"
        private const val COVER = "COVER"

        fun newInstance(albumTitle: String?, id: Int, cover: String): AlbumFragment {
            return AlbumFragment().apply {
                arguments = bundleOf(
                    TITLE to albumTitle,
                    ID to id,
                    COVER to cover
                )
            }
        }
    }
}
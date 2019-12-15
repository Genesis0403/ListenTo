package com.epam.listento.ui.albums

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
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
                        transportControls?.play()
                    AlbumViewModel.Command.PauseTrack ->
                        transportControls?.pause()
                }
            })

            currentPlaying.observe(viewLifecycleOwner, Observer<Int> {
                albumViewModel.handlePlayerStateChange(it)
            })

            playbackState.observe(
                viewLifecycleOwner,
                Observer<PlaybackState> {
                    handlePlayerStateChange(currentPlaying.value ?: -1)
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

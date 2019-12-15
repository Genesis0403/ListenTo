package com.epam.listento.ui.cache

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.model.CustomAlbum
import com.epam.listento.model.Track
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.ui.TracksAdapter
import com.epam.listento.ui.dialogs.TrackDialogDirections
import kotlinx.android.synthetic.main.cache_fragment.albumsRecyclerView
import kotlinx.android.synthetic.main.cache_fragment.albumsSection
import kotlinx.android.synthetic.main.tracks_fragment.progressBar
import kotlinx.android.synthetic.main.tracks_fragment.tracksRecyclerView
import javax.inject.Inject

class CacheFragment :
    Fragment(R.layout.cache_fragment),
    TracksAdapter.OnClickListener,
    AlbumsAdapter.OnClickListener {

    private val cacheViewModel: CacheScreenViewModel by activityViewModels {
        cacheFactory
    }

    private val navController by lazy(LazyThreadSafetyMode.NONE) {
        findNavController()
    }

    private val tracksAdapter = TracksAdapter(this)
    private val albumsAdapter = AlbumsAdapter(this)

    @Inject
    lateinit var cacheFactory: CacheScreenViewModel.Factory

    override fun onClick(track: Track) {
        cacheViewModel.handleTrackClick(track)
    }

    override fun onClick(album: CustomAlbum) {
        cacheViewModel.handleAlbumClick(album)
    }

    override fun onLongClick(track: Track) {
        navController.navigate(R.id.action_cacheFragmentNav_to_albumCreationActivity2)
    }

    override fun onLongClick(album: CustomAlbum) {
        cacheViewModel.handleAlbumLongClick(album)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        initObservers()
    }

    override fun onMenuClick(track: Track) {
        cacheViewModel.handleThreeDotButtonClick(track)
    }


    private fun initViews() {
        requireActivity().findViewById<Toolbar>(R.id.appToolBar)?.apply {
            menu.clear()
            inflateMenu(R.menu.search_toolbar_menu)
        }

        tracksRecyclerView.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = tracksAdapter
        }

        albumsRecyclerView.run {
            setHasFixedSize(true)
            adapter = albumsAdapter
        }
    }

    private fun initObservers() {
        with(cacheViewModel) {

            albums.observe(viewLifecycleOwner, Observer<List<CustomAlbum>> {
                Log.d(TAG, "Albums: $it")
                albumsSection.isVisible = if (it.isNullOrEmpty()) {
                    Log.d(TAG, "No albums out there :c")
                    false
                } else {
                    albumsAdapter.submitList(it)
                    true
                }
            })

            tracks.observe(viewLifecycleOwner, Observer<List<Track>> {
                val newData = it ?: emptyList()
                tracksAdapter.submitList(newData)
                progressBar.isVisible = false
            })

            currentPlaying.observe(viewLifecycleOwner, Observer<Int> {
                Log.d(TAG, "Current playing with id: $it")
                handlePlayerStateChange(it)
            })

            playbackState.observe(
                viewLifecycleOwner,
                Observer<PlaybackState> {
                    Log.d(TAG, "Current playback status is: $it")
                    handlePlayerStateChange(currentPlaying.value ?: -1)
                }
            )

            command.observe(
                viewLifecycleOwner,
                Observer<CacheScreenViewModel.Command> { action ->
                    when (action) {
                        CacheScreenViewModel.Command.ShowPlayerActivity ->
                            navController.navigate(R.id.playerActivity)
                        is CacheScreenViewModel.Command.PlayTrack ->
                            transportControls?.play()
                        is CacheScreenViewModel.Command.ShowAlbumActivity -> {
                            val direction =
                                CacheFragmentDirections.actionCacheFragmentNavToAlbumActivity(
                                    action.title,
                                    action.id,
                                    action.cover
                                )
                            navController.navigate(direction)
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
                }
            )
        }
    }

    companion object {
        private const val TAG = "CacheFragment"
    }
}

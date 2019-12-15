package com.epam.listento.ui.albums

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.model.Track
import com.epam.listento.ui.cache.CacheScreenViewModel
import com.epam.listento.ui.dialogs.TracksToAlbumAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import javax.inject.Inject

class AlbumCreationFragment :
    Fragment(R.layout.album_creation_fragment),
    TracksToAlbumAdapter.OnClickListener {

    private val cacheViewModel: CacheScreenViewModel by activityViewModels {
        cacheViewModelFactory
    }

    private val albumCreationViewModel: AlbumCreationViewModel by viewModels {
        albumsViewModelFactory
    }

    private val tracksAdapter: TracksToAlbumAdapter by lazy(LazyThreadSafetyMode.NONE) {
        TracksToAlbumAdapter(this)
    }

    private val intent = Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

    private lateinit var toolbar: Toolbar
    private lateinit var albumCover: ImageView
    private lateinit var albumTitleInputLayout: TextInputLayout
    private lateinit var albumArtistInputLayout: TextInputLayout
    private lateinit var albumTitleEditText: TextInputEditText
    private lateinit var albumArtistEditText: TextInputEditText
    private lateinit var tracksRecycler: RecyclerView

    @Inject
    lateinit var cacheViewModelFactory: CacheScreenViewModel.Factory

    @Inject
    lateinit var albumsViewModelFactory: AlbumCreationViewModel.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        initObservers()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "album cover url ${data?.dataString}")
            albumCreationViewModel.changeCover(data?.dataString)
        }
    }

    override fun onClick(track: Track) {
        albumCreationViewModel.onTrackClick(track)
    }

    private fun initViews(view: View) {
        with(view) {
            toolbar = findViewById(R.id.toolbar)
            albumCover = findViewById(R.id.albumCover)
            albumTitleInputLayout = findViewById(R.id.albumTitleInputLayout)
            albumArtistInputLayout = findViewById(R.id.albumArtistInputLayout)
            albumArtistEditText = findViewById(R.id.albumArtistEditText)
            albumTitleEditText = findViewById(R.id.albumTitleEditText)
            tracksRecycler = findViewById(R.id.tracksRecyclerView)
        }

        with(toolbar) {
            setOnMenuItemClickListener {
                albumCreationViewModel.onMenuItemClick(it.itemId)
            }
            setNavigationOnClickListener {
                albumCreationViewModel.onMenuItemClick(android.R.id.home)
            }
        }

        tracksRecycler.run {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tracksAdapter
        }
    }

    private fun initObservers() {
        with(cacheViewModel) {
            tracks.observe(viewLifecycleOwner, Observer<List<Track>> {
                val newData = it ?: emptyList()
                tracksAdapter.submitList(newData)
            })
        }

        with(albumCreationViewModel) {
            command.observe(viewLifecycleOwner, Observer<AlbumCreationViewModel.Command> {
                when (it) {
                    AlbumCreationViewModel.Command.ShowErrorOnQuery -> {
                        albumArtistInputLayout.error = getString(R.string.wront_album_name)
                        albumArtistInputLayout.error = getString(R.string.wrong_artist_name)
                    }
                    AlbumCreationViewModel.Command.SaveAlbum ->
                        albumCreationViewModel.saveAlbum(
                            albumTitleEditText.text.toString(),
                            albumArtistEditText.text.toString()
                        )
                    AlbumCreationViewModel.Command.ChangeCover ->
                        startActivityForResult(
                            intent,
                            REQUEST_CODE
                        )
                    AlbumCreationViewModel.Command.CloseActivity ->
                        requireActivity().finish()
                    is AlbumCreationViewModel.Command.ShowToast ->
                        Toast.makeText(
                            requireContext(),
                            it.message,
                            Toast.LENGTH_LONG
                        ).show()
                    is AlbumCreationViewModel.Command.LoadImage ->
                        loadImage(it.uri)
                }
            })
        }
    }

    private fun loadImage(url: String) {
        Glide.with(requireContext())
            .load(url)
            .error(R.drawable.anime)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(albumCover)
    }

    companion object {
        private const val TAG = "AlbumCreationFragment"
        private const val REQUEST_CODE = 404
    }
}

package com.epam.listento.ui.dialogs

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import javax.inject.Inject

class AlbumCreationDialog : DialogFragment(), TracksToAlbumAdapter.OnClickListener {

    private val cacheViewModel: CacheScreenViewModel by activityViewModels {
        cacheViewModelFactory
    }

    private val albumCreationViewModel: AlbumCreationViewModel by viewModels {
        albumsViewModelFactory
    }

    private val tracksAdapter: TracksToAlbumAdapter by lazy(LazyThreadSafetyMode.NONE) {
        TracksToAlbumAdapter(this)
    }

    private val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

    lateinit var toolbar: Toolbar
    lateinit var albumCover: ImageView
    lateinit var albumTitleInputLayout: TextInputLayout
    lateinit var albumArtistInputLayout: TextInputLayout
    lateinit var albumTitleEditText: TextInputEditText
    lateinit var albumArtistEditText: TextInputEditText
    lateinit var tracksRecycler: RecyclerView

    @Inject
    lateinit var cacheViewModelFactory: CacheScreenViewModel.Factory

    @Inject
    lateinit var albumsViewModelFactory: AlbumCreationViewModel.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
        setHasOptionsMenu(true)
        setStyle(STYLE_NORMAL, R.style.ListenTo_MaterialDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.album_creation_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        initObservers()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setWindowAnimations(R.style.AppTheme_SlideAnimation)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
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
                dismiss()
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
                    AlbumCreationViewModel.Command.SaveAlbum -> {
                        albumCreationViewModel.saveAlbum(
                            albumTitleEditText.text.toString(),
                            albumArtistEditText.text.toString()
                        )
                    }
                    AlbumCreationViewModel.Command.ChangeCover -> startActivityForResult(
                        intent,
                        REQUEST_CODE
                    )
                    AlbumCreationViewModel.Command.CloseDialog -> dismiss()
                    is AlbumCreationViewModel.Command.ShowToast -> Toast.makeText(
                        requireContext(),
                        it.message,
                        Toast.LENGTH_LONG
                    ).show()
                    is AlbumCreationViewModel.Command.LoadImage -> loadImage(it.uri)
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
        private const val TAG = "AlbumCreationDialog"
        private const val REQUEST_CODE = 404
        fun newInstance() = AlbumCreationDialog()
    }
}

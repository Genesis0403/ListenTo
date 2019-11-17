package com.epam.listento.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    private val tracksAdapter: TracksToAlbumAdapter by lazy(LazyThreadSafetyMode.NONE) {
        TracksToAlbumAdapter(this)
    }

    lateinit var toolbar: Toolbar
    lateinit var albumCover: ImageView
    lateinit var albumTitleInputLayout: TextInputLayout
    lateinit var albumArtistInputLayout: TextInputLayout
    lateinit var albumTitleEditText: TextInputEditText
    lateinit var albumArtistEditText: TextInputEditText
    lateinit var tracksRecycler: RecyclerView

    @Inject
    lateinit var cacheViewModelFactory: CacheScreenViewModel.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addImage -> {
                true
            }
            R.id.saveItem -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(track: Track) {
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

        toolbar.setNavigationOnClickListener {
            dismiss()
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
    }

    companion object {
        fun newInstance() = AlbumCreationDialog()
    }
}

package com.epam.listento.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.api.ApiResponse
import com.epam.listento.model.Track
import kotlinx.android.synthetic.main.tracks_fragment.*
import javax.inject.Inject

class TracksFragment : Fragment(), TracksAdapter.OnClickListener {

    companion object {
        private const val TAG = "TRACKS_FRAGMENT"

        fun newInstance() = TracksFragment()
    }

    @Inject
    lateinit var factory: ViewModelProvider.Factory

    private lateinit var mainViewModel: MainViewModel

    private val tracksAdapter = TracksAdapter(this)

    override fun onClick(track: Track) {
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        App.component.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProviders.of(requireActivity(), factory)[MainViewModel::class.java]
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

        downloadButton.setOnClickListener {
            mainViewModel.fetchTracks("джизус")
            progress.visibility = ProgressBar.VISIBLE
        }

        recycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = tracksAdapter

            mainViewModel.tracks.observe(
                this@TracksFragment,
                Observer<ApiResponse<List<Track>>> { response ->
                    initObserver(response)
                })
        }
    }

    private fun initObserver(response: ApiResponse<List<Track>>) {
        if (response.status.isSuccess()) {
            progress.visibility = ProgressBar.GONE
            tracksAdapter.setTracks(response.body ?: emptyList())
        } else {
            Toast.makeText(context, "Failed loading tracks", Toast.LENGTH_SHORT).show()
        }
    }
}
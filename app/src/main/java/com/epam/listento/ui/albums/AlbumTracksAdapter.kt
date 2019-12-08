package com.epam.listento.ui.albums

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.epam.listento.R
import com.epam.listento.model.Track
import com.epam.listento.model.durationToString

class AlbumTracksAdapter(
    private val listener: OnClickListener
) : ListAdapter<Track, AlbumTracksAdapter.TrackViewHolder>(Track.diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.album_track_item, parent, false)
        return TrackViewHolder(view) { listener.onClick(currentList[it]) }
    }

    override fun onBindViewHolder(
        holder: TrackViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        super.onBindViewHolder(holder, position, payloads)
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            payloads.forEach { payload ->
                when (payload) {
                    Track.RES_PAYLOAD -> {
                        val item = getItem(position)
                        showPlayback(holder.trackNumber, holder.playbackState, item.res)
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val item = currentList[position]
        holder.run {
            trackNumber.text = "${adapterPosition + 1}"
            trackTitle.text = item.title
            trackArtist.text = item.artist?.name
            duration.text = item.durationToString()
        }
    }

    private fun showPlayback(trackNumber: TextView, imageView: ImageView, iconId: Int) {
        imageView.isVisible = if (iconId == Track.NO_RES) {
            trackNumber.isVisible = true
            false
        } else {
            trackNumber.isVisible = false
            Glide.with(imageView)
                .load(iconId)
                .into(imageView)
            true
        }
    }

    class TrackViewHolder(
        view: View,
        clickListener: (position: Int) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        val trackNumber: TextView = view.findViewById(R.id.trackNumber)
        val trackTitle: TextView = view.findViewById(R.id.trackName)
        val trackArtist: TextView = view.findViewById(R.id.artistName)
        val duration: TextView = view.findViewById(R.id.duration)
        val playbackState: ImageView = view.findViewById(R.id.stateImage)

        private val cardView = view.findViewById<CardView>(R.id.cardView).also {
            it.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                clickListener.invoke(adapterPosition)
            }
        }
    }

    interface OnClickListener {
        fun onClick(track: Track)
    }
}

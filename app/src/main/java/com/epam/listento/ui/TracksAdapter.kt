package com.epam.listento.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.epam.listento.R
import com.epam.listento.model.Track
import com.epam.listento.model.Track.Companion.RES_PAYLOAD
import com.epam.listento.model.durationToString

private const val CORNERS_RADIUS = 16

class TracksAdapter(
    private val listener: OnClickListener
) : ListAdapter<Track, TracksAdapter.TrackViewHolder>(Track.diffCallback) {

    interface OnClickListener {
        fun onClick(track: Track)
        fun onLongClick(track: Track)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false)
        return TrackViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int, payloads: MutableList<Any>) {
        super.onBindViewHolder(holder, position, payloads)
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            payloads.forEach { payload ->
                when (payload) {
                    RES_PAYLOAD -> {
                        val item = getItem(position)
                        showPlayback(holder.playbackState, item.res)
                    }
                }
            }
        }
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val item = getItem(position)
        holder.apply {
            artist.text = item.artist?.name
            title.text = item.title
            duration.text = item.durationToString()
            loadImage(cover, item.album?.thumbnailCover)
            showPlayback(holder.playbackState, item.res)
        }
    }

    private fun loadImage(imageView: ImageView, url: String?) {
        Glide.with(imageView)
            .load(url)
            .fallback(R.drawable.no_photo_24dp)
            .error(R.drawable.no_photo_24dp)
            .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(CORNERS_RADIUS)))
            .into(imageView)
    }

    private fun showPlayback(imageView: ImageView, iconId: Int) {
        imageView.visibility = if (iconId == Track.NO_RES) {
            View.GONE
        } else {
            Glide.with(imageView)
                .load(iconId)
                .into(imageView)
            View.VISIBLE
        }
    }

    inner class TrackViewHolder(view: View, listener: OnClickListener) : RecyclerView.ViewHolder(view) {
        val cover: ImageView = view.findViewById(R.id.cover)
        val artist: TextView = view.findViewById(R.id.artist)
        val title: TextView = view.findViewById(R.id.title)
        val duration: TextView = view.findViewById(R.id.duration)
        val cardView: CardView = view.findViewById(R.id.cardView)
        val playbackState: ImageView = view.findViewById(R.id.playbackState)

        init {
            cardView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(adapterPosition)
                    listener.onClick(item)
                }
            }

            cardView.setOnLongClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val item = getItem(adapterPosition)
                    listener.onLongClick(item)
                    true
                } else {
                    false
                }
            }
        }
    }
}

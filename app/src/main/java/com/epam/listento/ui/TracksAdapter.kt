package com.epam.listento.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.epam.listento.R
import com.epam.listento.model.Track
import com.epam.listento.model.durationToString

private const val CORNERS_RADIUS = 16

class TracksAdapter(private val listener: OnClickListener) : RecyclerView.Adapter<TracksAdapter.TrackViewHolder>() {

    interface OnClickListener {
        fun onClick(track: Track)
    }

    private val tracks = mutableListOf<Track>()
    private var lastPlayed: TrackViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false)
        return TrackViewHolder(view).also { holder ->
            holder.cardView.setOnClickListener {
//                if (holder.isPlaying) {
//                    it.findNavController().navigate(R.id.playerActivity)
//                } else {
                    if (holder.adapterPosition != RecyclerView.NO_POSITION) {
//                        lastPlayed?.isPlaying = false
//                        lastPlayed = holder.also { it.isPlaying = true }
                        val item = tracks[holder.adapterPosition]
                        listener.onClick(item)
//                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return tracks.size
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val item = tracks[position]
        holder.apply {
            artist.text = item.artist?.name
            title.text = item.title
            duration.text = item.durationToString()
            loadImage(cover, item.album?.thumbnailCover)
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

    fun setTracks(data: List<Track>) {
        with(tracks) {
            clear()
            addAll(data)
            notifyDataSetChanged()
        }
    }

    class TrackViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cover: ImageView = view.findViewById(R.id.cover)
        val artist: TextView = view.findViewById(R.id.artist)
        val title: TextView = view.findViewById(R.id.title)
        val duration: TextView = view.findViewById(R.id.duration)
        val cardView: CardView = view.findViewById(R.id.cardView)
        var isPlaying = false
    }
}

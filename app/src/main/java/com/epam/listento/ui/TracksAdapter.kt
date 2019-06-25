package com.epam.listento.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.epam.listento.R
import com.epam.listento.model.Track

private const val TAG = "TRACKS_ADAPTER"

class TracksAdapter(private val listener: OnClickListener) : RecyclerView.Adapter<TracksAdapter.TrackViewHolder>() {

    interface OnClickListener {
        fun onClick(track: Track)
    }

    private val tracks = mutableListOf<Track>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.track_item, parent, false)
        return TrackViewHolder(view).also { holder ->
            holder.cardView.setOnClickListener {
                if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                    val item = tracks[holder.adapterPosition]
                    listener.onClick(item)
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
            duration.text = item.duration
            loadImage(cover, item.album?.listCover)
        }
    }

    private fun loadImage(imageView: ImageView, url: String?) {
        Glide.with(imageView)
            .load(url)
            .fallback(R.drawable.no_photo_24dp)
            .error(R.drawable.no_photo_24dp)
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
    }
}

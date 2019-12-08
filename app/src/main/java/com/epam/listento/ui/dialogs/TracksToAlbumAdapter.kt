package com.epam.listento.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.epam.listento.R
import com.epam.listento.model.Track
import com.epam.listento.model.durationToString

class TracksToAlbumAdapter(
    private val listener: OnClickListener
) : ListAdapter<Track, TracksToAlbumAdapter.TrackViewHolder>(Track.diffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.track_add_item,
            parent,
            false
        )
        return TrackViewHolder(view, listener, parent.context)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val item = getItem(position)
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

    inner class TrackViewHolder(
        view: View,
        private val listener: OnClickListener,
        private val context: Context
    ) : RecyclerView.ViewHolder(view) {
        var isChecked = false
            private set

        val cover: ImageView = view.findViewById(R.id.cover)
        val artist: TextView = view.findViewById(R.id.artist)
        val title: TextView = view.findViewById(R.id.title)
        val duration: TextView = view.findViewById(R.id.duration)
        val cardView: CardView = view.findViewById(R.id.cardView)
        //TODO omg remove this and change to selector
        private val color: Int

        init {
            cardView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
                changeCardColor()
                listener.onClick(getItem(adapterPosition))
            }
            color = cardView.cardBackgroundColor.defaultColor
        }

        private fun changeCardColor() {
            isChecked = !isChecked
            if (isChecked) {
                cardView.setCardBackgroundColor(
                    ContextCompat.getColor(
                        context,
                        R.color.checkedItem
                    )
                )
            } else {
                cardView.setCardBackgroundColor(color)
            }
        }
    }

    interface OnClickListener {
        fun onClick(track: Track)
    }

    private companion object {
        private const val CORNERS_RADIUS = 16
    }
}
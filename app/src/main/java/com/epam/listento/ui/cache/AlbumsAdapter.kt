package com.epam.listento.ui.cache

import android.util.Log
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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.epam.listento.R
import com.epam.listento.model.CustomAlbum

class AlbumsAdapter(
    private val listener: OnClickListener
) : ListAdapter<CustomAlbum, AlbumsAdapter.AlbumViewHolder>(CustomAlbum.diffUtil) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.album_item, parent, false)
        return AlbumViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        val item = getItem(position)
        Log.d(TAG, "Binding album")
        holder.run {
            title.text = item.title
            artist.text = item.artist
            loadImage(cover, item.cover)
        }
    }

    private fun loadImage(imageView: ImageView, url: String) {
        Glide.with(imageView)
            .load(url)
            .error(R.drawable.no_photo_24dp)
            .transition(DrawableTransitionOptions.withCrossFade())
            .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(CORNERS_RADIUS)))
            .into(imageView)
    }

    inner class AlbumViewHolder(
        view: View,
        listener: OnClickListener
    ) : RecyclerView.ViewHolder(view) {

        val cover: ImageView = view.findViewById(R.id.cover)
        val title: TextView = view.findViewById(R.id.title)
        val artist: TextView = view.findViewById(R.id.artist)
        val cardView: CardView = view.findViewById(R.id.cardView)

        init {
            cardView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener
            }

            cardView.setOnLongClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    false
                } else {
                    listener.onLongClick(getItem(adapterPosition))
                    true
                }
            }
        }
    }

    interface OnClickListener {
        fun onClick(album: CustomAlbum)
        fun onLongClick(album: CustomAlbum)
    }

    private companion object {
        private const val TAG = "AlbumsAdapter"
        private const val CORNERS_RADIUS = 16
    }
}

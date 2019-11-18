package com.epam.listento.model

import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CustomAlbum(
    val title: String,
    val artist: String,
    val cover: String,
    val tracks: List<Track>,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
) {
    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<CustomAlbum>() {
            override fun areItemsTheSame(oldItem: CustomAlbum, newItem: CustomAlbum): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: CustomAlbum, newItem: CustomAlbum): Boolean {
                return oldItem == newItem
            }
        }
    }
}
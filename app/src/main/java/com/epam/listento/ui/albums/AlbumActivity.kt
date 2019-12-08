package com.epam.listento.ui.albums

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.navigation.navArgs
import com.epam.listento.R

class AlbumActivity : AppCompatActivity(R.layout.album_activity) {

    private val args by navArgs<AlbumActivityArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.commit {
            add(
                R.id.albumActivity,
                AlbumFragment.newInstance(args.albumTitle, args.id, args.coverUrl),
                AlbumFragment.TAG
            )
        }
    }
}

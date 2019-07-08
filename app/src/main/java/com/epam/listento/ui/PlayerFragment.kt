package com.epam.listento.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.epam.listento.R
import kotlinx.android.synthetic.main.player_fragment.*

class PlayerFragment : Fragment() {

    companion object {
        fun newInstance() = PlayerFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.player_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val artist = view.findViewById<TextView>(R.id.artistName).also { it.text = "Джизус" }
        val title = view.findViewById<TextView>(R.id.trackTitle).also { it.text = "Девочка в классе" }

        positiveTiming.text = "0:00"
        negativeTiming.text = "-3:20"

        Glide.with(requireActivity())
            .load("https://images.genius.com/2e7e2475bf15d2fb2989adb5383a83e0.960x960x1.jpg")
            .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(28)))
            .into(albumCover)

        playButton.setOnClickListener {
            Toast.makeText(context, "CLICKED", Toast.LENGTH_SHORT).show()
        }

        forwardButton.setOnClickListener {
            Toast.makeText(context, "CLICKED FORWARD", Toast.LENGTH_SHORT).show()
        }

        rewindButton.setOnClickListener {
            Toast.makeText(context, "CLICKED REWIND", Toast.LENGTH_SHORT).show()
        }
    }
}

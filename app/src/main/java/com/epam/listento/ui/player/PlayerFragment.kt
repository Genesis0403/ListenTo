package com.epam.listento.ui.player

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.model.MsMapper
import com.epam.listento.model.player.PlaybackState
import kotlinx.android.synthetic.main.player_fragment.albumCover
import kotlinx.android.synthetic.main.player_fragment.artistName
import kotlinx.android.synthetic.main.player_fragment.backButton
import kotlinx.android.synthetic.main.player_fragment.forwardButton
import kotlinx.android.synthetic.main.player_fragment.negativeTiming
import kotlinx.android.synthetic.main.player_fragment.playButton
import kotlinx.android.synthetic.main.player_fragment.positiveTiming
import kotlinx.android.synthetic.main.player_fragment.rewindButton
import kotlinx.android.synthetic.main.player_fragment.trackTimeProgress
import kotlinx.android.synthetic.main.player_fragment.trackTitle
import javax.inject.Inject

class PlayerFragment : Fragment(R.layout.player_fragment) {

    private val playerViewModel: PlayerViewModel by viewModels {
        factory
    }

    private var isUserTouching = false

    @Inject
    lateinit var factory: PlayerViewModel.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trackTimeProgress.setOnSeekBarChangeListener(seekBarListener)

        playButton.setOnClickListener {
            playerViewModel.handleMediaButtonClick(R.id.playButton)
        }

        forwardButton.setOnClickListener {
            initOnSkipListener {
                playerViewModel.handleMediaButtonClick(R.id.forwardButton)
            }
        }

        rewindButton.setOnClickListener {
            initOnSkipListener {
                playerViewModel.handleMediaButtonClick(R.id.rewindButton)
            }
        }

        backButton.setOnClickListener {
            activity?.onBackPressed()
        }
        initObservers()
    }

    override fun onStop() {
        super.onStop()
        playerViewModel.stopScheduler()
    }

    private fun initOnSkipListener(action: () -> Unit) {
        val state = playerViewModel.serviceHelper.playbackState.value
        if (state == PlaybackState.Playing || state == PlaybackState.Paused) {
            action()
            trackTimeProgress.progress = 0
        }
    }

    private fun startScheduler() {
        playerViewModel.startScheduler {
            if (!isUserTouching) {
                trackTimeProgress.progress = playerViewModel.progress?.toInt() ?: 0
                changeSeekBarTimings(
                    trackTimeProgress.progress,
                    trackTimeProgress.max - trackTimeProgress.progress
                )
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun changeSeekBarTimings(positive: Int, negative: Int) {
        positiveTiming.text = MsMapper.convert(positive)
        negativeTiming.text = StringBuilder("-").append(MsMapper.convert(negative)).toString()
    }

    private fun initObservers() {
        with(playerViewModel) {

            serviceHelper.currentPlaying.observe(
                viewLifecycleOwner,
                Observer<Int> {
                    displayTrack(currentTrack)
                })

            serviceHelper.playbackState.observe(
                viewLifecycleOwner,
                Observer<PlaybackState> {
                    restorePlayButtonState(it)
                })

            command.observe(viewLifecycleOwner, Observer<PlayerViewModel.Command> {
                when (it) {
                    PlayerViewModel.Command.Play ->
                        playerViewModel.transportControls?.play()
                    PlayerViewModel.Command.Pause ->
                        playerViewModel.transportControls?.pause()
                    PlayerViewModel.Command.Forward ->
                        playerViewModel.transportControls?.skipToNext()
                    PlayerViewModel.Command.Backward ->
                        playerViewModel.transportControls?.skipToPrevious()
                    else -> playButton.isChecked = false
                }
            })
        }
    }

    private fun displayTrack(track: PlayerViewModel.MetadataTrack) {
        with(track) {
            trackTitle.text = title
            artistName.text = artist
            trackTimeProgress.max = duration.toInt()
            loadImage(cover)
            val positive = playerViewModel.progress?.toInt() ?: 0
            positiveTiming.text = MsMapper.convert(positive)
            negativeTiming.text = MsMapper.convert(duration.toInt())
        }
    }

    private fun loadImage(url: String?) {
        Glide.with(requireActivity())
            .load(url)
            .error(R.drawable.no_photo_24dp)
            .fallback(R.drawable.no_photo_24dp)
            .apply(RequestOptions().transform(CenterCrop(), RoundedCorners(CORNERS_RADIUS)))
            .into(albumCover)
    }

    private fun restorePlayButtonState(state: PlaybackState) {
        playButton.isChecked = when (state) {
            PlaybackState.Stopped, PlaybackState.Paused -> {
                playerViewModel.stopScheduler()
                false
            }
            PlaybackState.Playing -> {
                startScheduler()
                true
            }
            else -> false
        }
    }

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {

        private var resultProgress = 0

        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (seekBar != null) {
                if (fromUser) {
                    resultProgress = progress
                    changeSeekBarTimings(progress, seekBar.max - progress)
                }
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
            isUserTouching = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
            playerViewModel.transportControls?.seekTo(resultProgress.toLong())
            isUserTouching = false
        }
    }

    companion object {
        private const val TAG = "PlayerFragment"
        private const val CORNERS_RADIUS = 28
    }
}

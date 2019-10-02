package com.epam.listento.ui.player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.epam.listento.App
import com.epam.listento.R
import com.epam.listento.model.MsMapper
import com.epam.listento.model.PlayerService
import com.epam.listento.ui.viewmodels.PlayerViewModel
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

class PlayerFragment : Fragment() {

    companion object {
        private const val TAG = "PLAYER_FRAGMENT"
        private const val DEFAULT_TIMING = "0:00"
        private const val DEFAULT_TITLE = "None"
        private const val CORNERS_RADIUS = 28

        fun newInstance() = PlayerFragment()
    }

    @Inject
    lateinit var factory: PlayerViewModel.Factory
    private val playerViewModel: PlayerViewModel by viewModels {
        factory
    }

    private var binder: PlayerService.PlayerBinder? = null
    private var controller: MediaControllerCompat? = null

    private var isUserTouching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
        activity?.bindService(
            Intent(activity, PlayerService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
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

        trackTimeProgress.setOnSeekBarChangeListener(
            onSeekBarChangeListener()
        )

        playButton.setOnClickListener {
            listenToPlayerState()
        }

        forwardButton.setOnClickListener {
            initOnSkipListener {
                controller?.transportControls?.skipToNext()
            }
        }

        rewindButton.setOnClickListener {
            initOnSkipListener {
                controller?.transportControls?.skipToPrevious()
            }
        }

        backButton.setOnClickListener {
            activity?.onBackPressed()
        }
    }

    private fun initOnSkipListener(action: () -> Unit) {
        val state = controller?.playbackState?.state
        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            action()
            trackTimeProgress.progress = 0
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DESTROYED")
        activity?.unbindService(connection)
        controller?.unregisterCallback(controllerCallback)
        binder = null
        controller = null
        playerViewModel.stopScheduler()
    }

    private fun listenToPlayerState() {
        controller?.let {
            when (it.playbackState.state) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    it.transportControls.pause()
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    it.transportControls.play()
                }
                PlaybackStateCompat.STATE_STOPPED -> {
                    playButton.isChecked = false
                }
            }
        }
    }

    private fun restorePlayButtonState(state: PlaybackStateCompat?) {
        when (state?.state) {
            PlaybackStateCompat.STATE_STOPPED, PlaybackStateCompat.STATE_PAUSED -> {
                playerViewModel.stopScheduler()
                playButton.isChecked = false
            }
            PlaybackStateCompat.STATE_PLAYING -> {
                playButton.isChecked = true
                startScheduler()
            }
        }
    }

    private fun startScheduler() {
        playerViewModel.startScheduler {
            if (!isUserTouching) {
                trackTimeProgress.progress = binder?.getProgress()?.toInt() ?: 0
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

    private fun loadDataFromMetadata(metadata: MediaMetadataCompat?) {
        if (metadata?.description != null) {
            metadata.description.run {
                trackTitle.text = title
                artistName.text = subtitle
                val duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
                trackTimeProgress.max = duration
                loadImage(iconUri.toString())
                positiveTiming.text =
                    DEFAULT_TIMING
                negativeTiming.text = MsMapper.convert(duration)
            }
        } else {
            trackTitle.text = DEFAULT_TITLE
            artistName.text = DEFAULT_TITLE
            trackTimeProgress.max = 0
            loadImage(null)
            positiveTiming.text =
                DEFAULT_TIMING
            negativeTiming.text =
                DEFAULT_TIMING
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

    private fun onSeekBarChangeListener(): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {

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
                controller?.transportControls?.seekTo(resultProgress.toLong())
                isUserTouching = false
            }
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            binder = null
            controller?.unregisterCallback(controllerCallback)
            controller = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            binder = service as PlayerService.PlayerBinder
            binder?.let {
                val token = it.getSessionToken() ?: return
                controller = MediaControllerCompat(requireActivity(), token).also { controller ->
                    controller.registerCallback(
                        controllerCallback
                    )
                }
                restorePlayButtonState(controller?.playbackState)
                loadDataFromMetadata(controller?.metadata)
            }
        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            restorePlayButtonState(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            loadDataFromMetadata(metadata)
        }
    }
}

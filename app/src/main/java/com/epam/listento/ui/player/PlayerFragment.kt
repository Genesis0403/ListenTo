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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.epam.listento.model.PlayerService
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

class PlayerFragment : Fragment() {

    // TODO pass current playing track via bundle
    private val playerViewModel: PlayerViewModel by viewModels {
        factory
    }

    private var binder: PlayerService.PlayerBinder? = null
    private var controller: MediaControllerCompat? = null
    private var isUserTouching = false

    @Inject
    lateinit var factory: PlayerViewModel.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        App.component.inject(this)
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

        trackTimeProgress.setOnSeekBarChangeListener(seekBarListener)

        initObservers()

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

    override fun onStart() {
        super.onStart()
        activity?.bindService(
            Intent(activity, PlayerService::class.java),
            connection,
            Context.BIND_AUTO_CREATE
        )
    }

    override fun onStop() {
        super.onStop()
        activity?.unbindService(connection)
        controller?.unregisterCallback(controllerCallback)
        binder = null
        controller = null
        playerViewModel.stopScheduler()
    }

    private fun initOnSkipListener(action: () -> Unit) {
        val state = controller?.playbackState?.state
        if (state == PlaybackStateCompat.STATE_PLAYING || state == PlaybackStateCompat.STATE_PAUSED) {
            action()
            trackTimeProgress.progress = 0
        }
    }

    private fun listenToPlayerState() {
        controller?.let {
            when (playerViewModel.playbackState.value) {
                PlaybackState.Playing -> {
                    it.transportControls.pause()
                }
                PlaybackState.Paused -> {
                    it.transportControls.play()
                }
                else -> {
                    playButton.isChecked = false
                }
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

    private fun initObservers() {
        with(playerViewModel) {

            currentPlaying.observe(viewLifecycleOwner, Observer<PlayerViewModel.MetadataTrack> {
                displayTrack(it)
            })

            playbackState.observe(viewLifecycleOwner, Observer<PlaybackState> {
                restorePlayButtonState(it)
            })
        }
    }

    private fun displayTrack(track: PlayerViewModel.MetadataTrack) {
        with(track) {
            trackTitle.text = title
            artistName.text = artist
            trackTimeProgress.max = duration.toInt()
            loadImage(cover)
            val positive = binder?.getProgress()?.toInt() ?: 0
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
                    controller.registerCallback(controllerCallback)
                }
            }
        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            playerViewModel.handlePlaybackStateChange(
                state?.state ?: PlaybackStateCompat.STATE_NONE
            )
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            playerViewModel.handleMetadataChange(metadata?.toMetadataTrack() ?: return)
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
            controller?.transportControls?.seekTo(resultProgress.toLong())
            isUserTouching = false
        }
    }

    companion object {
        private const val TAG = "PlayerFragment"
        private const val CORNERS_RADIUS = 28

        fun newInstance() = PlayerFragment()
    }
}

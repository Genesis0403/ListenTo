package com.epam.listento.model

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.epam.listento.api.ApiResponse
import com.epam.listento.model.player.utils.id
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.TestDispatchers
import com.google.android.exoplayer2.SimpleExoPlayer
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
object MediaSessionCallbackSpek : Spek({

    val dispatchers = TestDispatchers()
    val context = mockk<Context>(relaxed = true)
    val musicRepo = mockk<MusicRepository>(relaxed = true, relaxUnitFun = true)
    val downloadInteractor = mockk<DownloadInteractor>(relaxed = true, relaxUnitFun = true)
    val player = mockk<SimpleExoPlayer>(relaxed = true)
    val onSessionUpdate: (MediaMetadataCompat?, Boolean, Int) -> Unit = mockk(
        relaxed = true,
        relaxUnitFun = true
    )

    lateinit var callback: MediaSessionCallback

    fun createCallback() {
        callback = MediaSessionCallback(
            dispatchers,
            context,
            musicRepo,
            downloadInteractor,
            player,
            onSessionUpdate
        )
    }

    val metadata = mockk<MediaMetadataCompat>(relaxed = true)

    describe("play media buttons actions") {

        beforeEachTest {
            createCallback()
        }

        afterEachTest {
            clearMocks(player, context, downloadInteractor, onSessionUpdate)
        }

        it("should play track") {
            every { player.playWhenReady } returns false
            every { musicRepo.getCurrent() } returns metadata
            coEvery { downloadInteractor.fillMetadata(metadata) } returns metadata
            every { downloadInteractor.isCaching() } returns true
            coEvery {
                downloadInteractor.downloadTrack(metadata, true)
            } returns ApiResponse.success(null)

            callback.onPlay()

            assertTrue {
                callback.currentState == PlaybackStateCompat.STATE_PLAYING &&
                        callback.currentPlaying?.id == metadata.id
            }
            verify { onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING) }
        }

        it("should resume track") {
            every { player.playWhenReady } returns true
            every { musicRepo.getCurrent() } returns metadata
            coEvery { downloadInteractor.fillMetadata(metadata) } returns metadata

            callback.onPause()
            callback.onPlay()

            verify { onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING) }
        }

        it("should pause track when playing") {
            every { player.playWhenReady } returns true
            every { musicRepo.getCurrent() } returns metadata

            callback.onPause()

            verify { onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PAUSED) }
        }

        it("should return when track is already pause") {
            every { player.playWhenReady } returns false

            callback.onPause()

            verify(inverse = true) {
                onSessionUpdate(any(), true, PlaybackStateCompat.STATE_PAUSED)
            }
        }

        it("should stop playing") {
            every { player.playWhenReady } returns true

            callback.onStop()

            verify {
                onSessionUpdate(any(), false, PlaybackStateCompat.STATE_STOPPED)
            }
        }

        it("should not stop if it's already stopped") {
            every { player.playWhenReady } returns false

            callback.onStop()

            verify(inverse = true) {
                onSessionUpdate(any(), false, PlaybackStateCompat.STATE_STOPPED)
            }
        }

        it("should skip to next track") {
            every { player.playWhenReady } returns true
            every { musicRepo.getNext() } returns metadata
            coEvery { downloadInteractor.fillMetadata(metadata) } returns metadata
            every { downloadInteractor.isCaching() } returns true
            coEvery {
                downloadInteractor.downloadTrack(metadata, true)
            } returns ApiResponse.success(null)

            callback.onSkipToNext()
            verify { onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING) }
        }

        it("should resume track when skip to next") {
            every { player.playWhenReady } returns true
            every { musicRepo.getNext() } returns metadata
            coEvery { downloadInteractor.fillMetadata(metadata) } returns metadata

            callback.onPause()
            callback.onSkipToNext()

            verify { onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING) }
        }

        it("should skip to previous track") {
            every { player.playWhenReady } returns true
            every { musicRepo.getPrevious() } returns metadata
            coEvery { downloadInteractor.fillMetadata(metadata) } returns metadata
            every { downloadInteractor.isCaching() } returns true
            coEvery {
                downloadInteractor.downloadTrack(metadata, true)
            } returns ApiResponse.success(null)

            callback.onSkipToPrevious()
            verify { onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING) }
        }

        it("should resume track when skip to next") {
            every { player.playWhenReady } returns true
            every { musicRepo.getPrevious() } returns metadata
            coEvery { downloadInteractor.fillMetadata(metadata) } returns metadata

            callback.onPause()
            callback.onSkipToPrevious()

            verify { onSessionUpdate(metadata, true, PlaybackStateCompat.STATE_PLAYING) }
        }
    }
    
    describe("player seeking") {
        
        val mockedPosition = 151L

        it("should seek") {
            callback.onSeekTo(mockedPosition)
            verify { player.seekTo(mockedPosition) }
        }
    }
})
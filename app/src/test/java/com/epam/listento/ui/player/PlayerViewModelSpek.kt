package com.epam.listento.ui.player

import androidx.lifecycle.Observer
import com.epam.listento.R
import com.epam.listento.ServiceHelper
import com.epam.listento.model.player.PlaybackState
import com.epam.listento.repository.global.MusicRepository
import com.epam.listento.utils.TestDispatchers
import com.epam.listento.utils.emulateInstanteTaskExecutorRule
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import kotlin.test.assertTrue

@RunWith(JUnitPlatform::class)
object PlayerViewModelSpek : Spek({

    emulateInstanteTaskExecutorRule()

    val serviceHelper = mockk<ServiceHelper>(relaxed = true)
    val musicRepository = mockk<MusicRepository>(relaxed = true)
    val dispatchers = TestDispatchers()

    val commandObserver: Observer<PlayerViewModel.Command> = mockk(relaxed = true)

    lateinit var viewModel: PlayerViewModel

    fun createViewModel() {
        viewModel = PlayerViewModel(
            serviceHelper,
            musicRepository,
            dispatchers
        )
    }

    describe("media button clicks") {

        beforeEachTest {
            createViewModel()
            viewModel.command.observeForever(commandObserver)
        }

        afterEachTest {
            viewModel.command.removeObserver(commandObserver)
            clearMocks(commandObserver, serviceHelper)
        }

        it("should be forward button click") {
            viewModel.handleMediaButtonClick(R.id.forwardButton)
            verify { commandObserver.onChanged(PlayerViewModel.Command.Forward) }
        }

        it("should be backward button click") {
            viewModel.handleMediaButtonClick(R.id.rewindButton)
            verify { commandObserver.onChanged(PlayerViewModel.Command.Backward) }
        }

        it("should be play button click") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Paused
            viewModel.handleMediaButtonClick(R.id.playButton)
            verify { commandObserver.onChanged(PlayerViewModel.Command.Play) }
        }

        it("should be pause button click") {
            every { serviceHelper.playbackState.value } returns PlaybackState.Playing
            viewModel.handleMediaButtonClick(R.id.playButton)
            verify { commandObserver.onChanged(PlayerViewModel.Command.Pause) }
        }
    }

    describe("timer actions") {

        val mockedAction: () -> Unit = mockk(relaxed = true, relaxUnitFun = true)

        it("should start") {
            viewModel.startScheduler(mockedAction)
            verify { mockedAction.invoke() }
        }

        it("should stop") {
            viewModel.stopScheduler()
            assertTrue { true }
        }
    }
})

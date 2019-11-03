package com.epam.listento.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Observer
import com.epam.listento.model.CacheInteractor
import com.epam.listento.model.DownloadInteractor
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.ContextProvider
import com.epam.listento.utils.emulateInstanteTaskExecutorRule
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@RunWith(JUnitPlatform::class)
object MainViewModelSpek : Spek({

    emulateInstanteTaskExecutorRule()

    val contextPovider: ContextProvider = mockk(relaxed = true)
    val cacheInteractor: CacheInteractor = mockk(relaxed = true)
    val downloadInteractor: DownloadInteractor = mockk(relaxed = true)
    val dispatchers: AppDispatchers = mockk(relaxed = true)

    var nightObserver: Observer<Int> = mockk(relaxed = true)

    lateinit var viewModel: MainViewModel

    val nightModeKey = "night_mode"

    fun createViewModel() {
        viewModel = MainViewModel(
            contextPovider,
            cacheInteractor,
            downloadInteractor,
            dispatchers
        )
    }

    beforeEachTest {
        every { dispatchers.default } returns Dispatchers.Unconfined
        every { dispatchers.ui } returns Dispatchers.Unconfined
        every { dispatchers.io } returns Dispatchers.Unconfined
    }

    afterEachTest {
        clearMocks(dispatchers)
    }

    describe("theme change") {

        beforeEachTest {
            createViewModel()
            nightObserver = mockk(relaxed = true)
            viewModel.nightMode.observeForever(nightObserver)
        }

        afterEachTest {
            clearMocks(nightObserver)
        }

        it("key is not night mode key") {
            val notNightModeKey = "not_night_mode_key"
            viewModel.handleThemeChange(false, notNightModeKey)
            verify(inverse = true) { nightObserver.onChanged(anyInt()) }
        }

        it("night mode should be enabled") {
            viewModel.handleThemeChange(true, nightModeKey)
            verify { nightObserver.onChanged(AppCompatDelegate.MODE_NIGHT_YES) }
        }

        it("night mode should not be enabled") {
            viewModel.handleThemeChange(false, nightModeKey)
            verify { nightObserver.onChanged(AppCompatDelegate.MODE_NIGHT_NO) }
        }
    }
})

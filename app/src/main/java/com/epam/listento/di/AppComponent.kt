package com.epam.listento.di

import android.app.Application
import com.epam.listento.ui.MainActivity
import com.epam.listento.ui.TracksFragment
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class
    ]
)
interface AppComponent {

    @Component.Builder
    interface Builder {
        fun build(): AppComponent

        @BindsInstance
        fun application(app: Application): Builder
    }

    fun inject(activity: MainActivity)
    fun inject(fragment: TracksFragment)
}

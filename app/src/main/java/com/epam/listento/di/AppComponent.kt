package com.epam.listento.di

import android.app.Application
import com.epam.listento.model.PlayerService
import com.epam.listento.ui.*
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
    fun inject(fragment: SearchFragment)
    fun inject(fragment: PlayerFragment)
    fun inject(service: PlayerService)
    fun inject(fragment: PlaylistFragment)
    fun inject(fragment: TrackDialog)
    fun inject(fragment: PreferencesFragment)
}

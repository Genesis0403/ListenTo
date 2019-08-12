package com.epam.listento.di

import android.app.Application
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import dagger.Module
import dagger.Provides

@Module
class PlayerModule {

    @Provides
    fun provideExoPlayer(app: Application): SimpleExoPlayer {
        return ExoPlayerFactory.newSimpleInstance(app)
    }
}
package com.epam.listento.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.epam.listento.ui.viewmodels.MainViewModel
import com.epam.listento.ui.viewmodels.PlayerViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class ViewModelModule {

    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    abstract fun provideMainViewModel(viewModel: MainViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(PlayerViewModel::class)
    abstract fun providePlayerViewModel(viewModel: PlayerViewModel): ViewModel
}

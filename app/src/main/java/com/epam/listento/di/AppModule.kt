package com.epam.listento.di

import android.app.Application
import android.content.Context
import com.epam.listento.utils.ContextProvider
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    includes = [
        ViewModelModule::class,
        RepositoryModule::class
    ]
)
class AppModule {

    @Singleton
    @Provides
    fun provideContextProvider(app: Application): ContextProvider {
        return object : ContextProvider {
            override fun context(): Context {
                return app
            }

            override fun getString(id: Int): String {
                return app.getString(id)
            }
        }
    }
}
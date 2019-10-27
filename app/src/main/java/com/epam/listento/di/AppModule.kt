package com.epam.listento.di

import android.app.Application
import android.content.Context
import com.epam.listento.utils.AppDispatchers
import com.epam.listento.utils.ContextProvider
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module(
    includes = [
        RepositoryModule::class,
        PlayerModule::class
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

    @Singleton
    @Provides
    fun provideDispatchers(): AppDispatchers {
        return object : AppDispatchers {
            override val ui: CoroutineDispatcher get() = Dispatchers.Main

            override val io: CoroutineDispatcher get() = Dispatchers.IO

            override val default: CoroutineDispatcher get() = Dispatchers.Default
        }
    }
}

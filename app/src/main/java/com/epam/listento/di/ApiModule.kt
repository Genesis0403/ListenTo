package com.epam.listento.di

import android.app.Application
import com.epam.listento.R
import com.epam.listento.api.YandexService
import com.epam.listento.utils.JsonXmlConverter
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
class ApiModule {

    @Singleton
    @Provides
    fun provideRetrofit(app: Application, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(JsonXmlConverter())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .client(client)
            .baseUrl(app.getString(R.string.baseUrl))
            .build()
    }

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().also {
                it.level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Singleton
    @Provides
    fun provideYandexService(retrofit: Retrofit): YandexService {
        return retrofit.create(YandexService::class.java)
    }
}

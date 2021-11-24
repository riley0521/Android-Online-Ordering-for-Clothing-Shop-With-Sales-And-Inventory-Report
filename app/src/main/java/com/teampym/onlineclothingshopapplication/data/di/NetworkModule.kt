package com.teampym.onlineclothingshopapplication.data.di

import com.teampym.onlineclothingshopapplication.data.network.FCMService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://fcm.googleapis.com/fcm/send"

    @Provides
    @Singleton
    fun provideRetrofitInstance() =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .build()

    @Provides
    @Singleton
    fun provideFCMService(
        retrofit: Retrofit
    ) = retrofit.create(FCMService::class.java)
}

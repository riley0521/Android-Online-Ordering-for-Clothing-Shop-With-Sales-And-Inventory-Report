package com.teampym.onlineclothingshopapplication.data.di

import com.teampym.onlineclothingshopapplication.data.network.FCMService
import com.teampym.onlineclothingshopapplication.data.network.GoogleSheetService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

private const val BASE_URL = "https://fcm.googleapis.com/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofitInstance(): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .build()

    @Provides
    @Singleton
    fun provideFCMService(
        retrofit: Retrofit
    ): FCMService = retrofit.create(FCMService::class.java)

    @Provides
    @Singleton
    fun provideGoogleSheetService(): GoogleSheetService {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://script.google.com/macros/s/AKfycbzO4KxLqmqRBqb5ymAOxk0qURUUOETKHid_LTBDlFBjl6INPOPX1Q5WwQ8mm2dbTHgVQA/")
            .build()

        return retrofit.create(GoogleSheetService::class.java)
    }
}

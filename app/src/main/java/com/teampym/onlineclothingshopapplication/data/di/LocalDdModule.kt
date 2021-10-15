package com.teampym.onlineclothingshopapplication.data.di

import android.app.Application
import androidx.room.Room
import com.teampym.onlineclothingshopapplication.data.db.MyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDbModule {

    @Provides
    @Singleton
    fun provideDatabase(
        app: Application
    ) = Room.databaseBuilder(app, MyDatabase::class.java, "my_database")
        .fallbackToDestructiveMigration()
        .build()

}
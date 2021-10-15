package com.teampym.onlineclothingshopapplication.data.di

import android.app.Application
import androidx.room.Room
import com.teampym.onlineclothingshopapplication.data.db.MyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocalDbModule {

    @Provides
    @Singleton
    fun provideDatabase(
        app: Application,
        callback: MyDatabase.Callback
    ) = Room.databaseBuilder(app, MyDatabase::class.java, "my_database")
        .fallbackToDestructiveMigration()
        .addCallback(callback)
        .build()

    @Provides
    @Singleton
    fun provideRegionDao(db: MyDatabase) = db.regionDao()

    @Provides
    @Singleton
    fun provideProvinceDao(db: MyDatabase) = db.provinceDao()

    @Provides
    @Singleton
    fun provideCityDao(db: MyDatabase) = db.cityDao()

    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())

}
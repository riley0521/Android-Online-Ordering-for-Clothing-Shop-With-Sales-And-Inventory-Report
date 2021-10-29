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
import javax.inject.Qualifier
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
    fun provideUserInformationDao(db: MyDatabase) = db.userInformationDao()

    @Provides
    @Singleton
    fun provideDeliveryInformationDao(db: MyDatabase) = db.deliveryInformationDao()

    @Provides
    @Singleton
    fun provideNotificationTokenDao(db: MyDatabase) = db.notificationTokenDao()

    @Provides
    @Singleton
    fun provideCartDao(db: MyDatabase) = db.cartDao()

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob())

}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope
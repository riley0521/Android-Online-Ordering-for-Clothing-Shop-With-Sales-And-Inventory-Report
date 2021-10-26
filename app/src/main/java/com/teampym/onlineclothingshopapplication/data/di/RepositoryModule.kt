package com.teampym.onlineclothingshopapplication.data.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.teampym.onlineclothingshopapplication.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDbInstance() =
        Firebase.firestore

    @Provides
    @Singleton
    fun provideAccountAndDeliveryInformationRepository(db: FirebaseFirestore) =
        AccountAndDeliveryInformationImpl(db)

    @Provides
    @Singleton
    fun provideCartRepository(db: FirebaseFirestore) =
        CartRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideCategoryRepository(db: FirebaseFirestore) =
        CategoryRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideOrderRepository(db: FirebaseFirestore) =
        OrderRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideProductRepository(db: FirebaseFirestore) =
        ProductWithInventoryAndImagesRepositoryImpl(db)

}
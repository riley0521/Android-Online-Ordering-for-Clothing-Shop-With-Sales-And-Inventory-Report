package com.teampym.onlineclothingshopapplication.data.di

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.teampym.onlineclothingshopapplication.data.repository.AccountRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.repository.CategoryRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.repository.OrderRepositoryImpl
import com.teampym.onlineclothingshopapplication.data.repository.ProductRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideCategoryRepository(db: FirebaseFirestore) =
        CategoryRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideProductRepository(db: FirebaseFirestore) =
        ProductRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideOrderRepository(db: FirebaseFirestore) =
        OrderRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideAccountRepository(db: FirebaseFirestore) =
        AccountRepositoryImpl(db)
}
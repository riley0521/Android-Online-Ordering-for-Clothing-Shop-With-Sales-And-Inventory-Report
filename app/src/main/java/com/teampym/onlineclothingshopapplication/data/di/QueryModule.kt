package com.teampym.onlineclothingshopapplication.data.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.teampym.onlineclothingshopapplication.data.util.ORDERS_COLLECTION
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object QueryModule {

    @Provides
    @Singleton
    @Named("orders")
    fun provideQueryOrdersByDate(db: FirebaseFirestore) =
        db.collection(ORDERS_COLLECTION)
            .orderBy("orderDate", Query.Direction.ASCENDING)
            .limit(30)
}
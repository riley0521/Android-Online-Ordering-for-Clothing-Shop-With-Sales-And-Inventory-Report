package com.teampym.onlineclothingshopapplication.data.di

import com.google.firebase.firestore.FirebaseFirestore
import com.teampym.onlineclothingshopapplication.data.db.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.db.NotificationTokenDao
import com.teampym.onlineclothingshopapplication.data.db.UserInformationDao
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
        FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideAccountAndDeliveryInformationRepository(
        db: FirebaseFirestore,
        userInformationDao: UserInformationDao,
        deliveryInformationDao: DeliveryInformationDao,
        notificationTokenDao: NotificationTokenDao
    ) = AccountAndDeliveryInformationImpl(
        db,
        userInformationDao,
        deliveryInformationDao,
        notificationTokenDao
    )

    @Provides
    @Singleton
    fun provideCartRepository(db: FirebaseFirestore, userInformationDao: UserInformationDao) =
        CartRepositoryImpl(db, userInformationDao)

    @Provides
    @Singleton
    fun provideCategoryRepository(db: FirebaseFirestore) =
        CategoryRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideOrderRepository(
        db: FirebaseFirestore,
        cartRepository: CartRepositoryImpl,
        productRepository: ProductImageWithInventoryAndReviewRepositoryImpl
    ) = OrderRepositoryImpl(db, cartRepository, productRepository)

    @Provides
    @Singleton
    fun provideProductRepository(
        db: FirebaseFirestore,
        accountRepository: AccountAndDeliveryInformationImpl
    ) =
        ProductImageWithInventoryAndReviewRepositoryImpl(db, accountRepository)

}
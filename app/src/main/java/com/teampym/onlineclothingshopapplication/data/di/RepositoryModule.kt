package com.teampym.onlineclothingshopapplication.data.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.teampym.onlineclothingshopapplication.data.db.CartDao
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
        Firebase.firestore

    @Provides
    @Singleton
    fun provideCategoryRepository(db: FirebaseFirestore) =
        CategoryRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideAccountRepository(
        db: FirebaseFirestore,
        deliveryInformationRepository: DeliveryInformationRepositoryImpl,
        notificationTokenRepository: NotificationTokenRepositoryImpl,
        cartRepository: CartRepositoryImpl,
        userInformationDao: UserInformationDao
    ) = AccountRepositoryImpl(
        db,
        deliveryInformationRepository,
        notificationTokenRepository,
        cartRepository,
        userInformationDao
    )

    @Provides
    @Singleton
    fun provideDeliveryInformationRepository(
        db: FirebaseFirestore,
        deliveryInformationDao: DeliveryInformationDao
    ) = DeliveryInformationRepositoryImpl(
        db,
        deliveryInformationDao
    )

    @Provides
    @Singleton
    fun provideNotificationTokenRepository(
        db: FirebaseFirestore,
        notificationTokenDao: NotificationTokenDao
    ) = NotificationTokenRepositoryImpl(
        db,
        notificationTokenDao
    )

    @Provides
    @Singleton
    fun provideCartRepository(db: FirebaseFirestore, cartDao: CartDao) =
        CartRepositoryImpl(db, cartDao)

    @Provides
    @Singleton
    fun provideProductRepository(
        db: FirebaseFirestore,
        productImageRepository: ProductImageRepositoryImpl,
        productInventoryRepository: ProductInventoryRepositoryImpl,
        reviewRepository: ReviewRepositoryImpl,
        accountRepository: AccountRepositoryImpl
    ) = ProductRepositoryImpl(
        db,
        productImageRepository,
        productInventoryRepository,
        reviewRepository,
        accountRepository
    )

    @Provides
    @Singleton
    fun provideProductImageRepository(
        db: FirebaseFirestore
    ) = ProductImageRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideProductInventoryRepository(
        db: FirebaseFirestore
    ) = ProductInventoryRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideReviewRepository(
        db: FirebaseFirestore
    ) = ReviewRepositoryImpl(db)

    @Provides
    @Singleton
    fun provideOrderRepository(
        db: FirebaseFirestore,
        orderDetailRepository: OrderDetailRepositoryImpl,
        cartRepository: CartRepositoryImpl,
        productRepository: ProductRepositoryImpl
    ) = OrderRepositoryImpl(db, orderDetailRepository, cartRepository, productRepository)

    @Provides
    @Singleton
    fun provideOrderDetailRepository(
        db: FirebaseFirestore
    ) = OrderDetailRepositoryImpl(
        db
    )

}
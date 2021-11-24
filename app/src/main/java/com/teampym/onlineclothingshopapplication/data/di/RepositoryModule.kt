package com.teampym.onlineclothingshopapplication.data.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.teampym.onlineclothingshopapplication.data.repository.* // ktlint-disable no-wildcard-imports
import com.teampym.onlineclothingshopapplication.data.room.CartDao
import com.teampym.onlineclothingshopapplication.data.room.DeliveryInformationDao
import com.teampym.onlineclothingshopapplication.data.room.InventoryDao
import com.teampym.onlineclothingshopapplication.data.room.NotificationTokenDao
import com.teampym.onlineclothingshopapplication.data.room.PreferencesManager
import com.teampym.onlineclothingshopapplication.data.room.ProductDao
import com.teampym.onlineclothingshopapplication.data.room.UserInformationDao
import com.teampym.onlineclothingshopapplication.data.room.WishItemDao
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
        wishItemRepository: WishItemRepositoryImpl,
        userInformationDao: UserInformationDao
    ) = AccountRepositoryImpl(
        db,
        wishItemRepository,
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
        notificationTokenDao: NotificationTokenDao,
        preferencesManager: PreferencesManager
    ) = NotificationTokenRepositoryImpl(
        db,
        notificationTokenDao,
        preferencesManager
    )

    @Provides
    @Singleton
    fun provideWishListRepository(
        db: FirebaseFirestore,
        wishItemDao: WishItemDao
    ) = WishItemRepositoryImpl(db, wishItemDao)

    @Provides
    @Singleton
    fun provideCartRepository(
        db: FirebaseFirestore,
        cartDao: CartDao
    ) = CartRepositoryImpl(db, cartDao)

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
        notificationTokenRepository: NotificationTokenRepositoryImpl,
        orderDetailRepository: OrderDetailRepositoryImpl,
        productRepository: ProductRepositoryImpl
    ) = OrderRepositoryImpl(
        db,
        notificationTokenRepository,
        orderDetailRepository,
        productRepository
    )

    @Provides
    @Singleton
    fun provideOrderDetailRepository(
        db: FirebaseFirestore
    ) = OrderDetailRepositoryImpl(
        db
    )

    @Provides
    @Singleton
    fun providePostRepository(
        db: FirebaseFirestore
    ) = PostRepositoryImpl(
        db
    )

    @Provides
    @Singleton
    fun provideLikeRepository(
        db: FirebaseFirestore,
        postRepository: PostRepositoryImpl
    ) = LikeRepositoryImpl(
        db,
        postRepository
    )

    @Provides
    @Singleton
    fun provideCommentRepository(
        db: FirebaseFirestore,
        postRepository: PostRepositoryImpl
    ) = CommentRepositoryImpl(
        db,
        postRepository
    )

    @Provides
    @Singleton
    fun provideAuditTrailRepository(
        db: FirebaseFirestore
    ) = AuditTrailRepositoryImpl(
        db
    )
}

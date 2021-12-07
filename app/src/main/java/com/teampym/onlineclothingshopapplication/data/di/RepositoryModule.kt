package com.teampym.onlineclothingshopapplication.data.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.teampym.onlineclothingshopapplication.data.repository.* // ktlint-disable no-wildcard-imports
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideDbInstance() =
        Firebase.firestore

    @IoDispatcher
    @Provides
    fun provideIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    fun provideCategoryRepository(
        db: FirebaseFirestore,
        productRepository: ProductRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = CategoryRepository(db, productRepository, dispatcher)

    @Provides
    fun provideAccountRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = AccountRepository(
        db,
        dispatcher
    )

    @Provides
    fun provideDeliveryInformationRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = DeliveryInformationRepository(
        db,
        dispatcher
    )

    @Provides
    fun provideNotificationTokenRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = NotificationTokenRepositoryImpl(
        db,
        dispatcher
    )

    @Provides
    fun provideWishListRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = WishItemRepository(db, dispatcher)

    @Provides
    fun provideCartRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = CartRepository(db, dispatcher)

    @Provides
    fun provideProductRepository(
        db: FirebaseFirestore,
        productImageRepository: ProductImageRepository,
        productInventoryRepository: ProductInventoryRepository,
        reviewRepository: ReviewRepository,
        notificationTokenRepository: NotificationTokenRepositoryImpl,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = ProductRepository(
        db,
        productImageRepository,
        productInventoryRepository,
        reviewRepository,
        notificationTokenRepository,
        dispatcher
    )

    @Provides
    fun provideProductImageRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = ProductImageRepository(db, dispatcher)

    @Provides
    fun provideProductInventoryRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = ProductInventoryRepository(db, dispatcher)

    @Provides
    fun provideReviewRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = ReviewRepository(db, dispatcher)

    @Provides
    fun provideOrderRepository(
        db: FirebaseFirestore,
        orderDetailRepository: OrderDetailRepository,
        productRepository: ProductRepository,
        notificationTokenRepository: NotificationTokenRepositoryImpl,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = OrderRepository(
        db,
        orderDetailRepository,
        productRepository,
        notificationTokenRepository,
        dispatcher
    )

    @Provides
    fun provideOrderDetailRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = OrderDetailRepository(
        db,
        dispatcher
    )

    @Provides
    fun providePostRepository(
        db: FirebaseFirestore,
        notificationTokenRepository: NotificationTokenRepositoryImpl,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = PostRepository(
        db,
        notificationTokenRepository,
        dispatcher
    )

    @Provides
    fun provideLikeRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = LikeRepository(
        db,
        dispatcher
    )

    @Provides
    fun provideCommentRepository(
        db: FirebaseFirestore,
        notificationTokenRepository: NotificationTokenRepositoryImpl,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = CommentRepository(
        db,
        notificationTokenRepository,
        dispatcher
    )

    @Provides
    fun provideAuditTrailRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = AuditTrailRepository(
        db,
        dispatcher
    )
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class IoDispatcher

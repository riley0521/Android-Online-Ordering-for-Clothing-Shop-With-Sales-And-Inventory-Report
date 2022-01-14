package com.teampym.onlineclothingshopapplication.data.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.teampym.onlineclothingshopapplication.data.repository.*
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
        auditTrailRepository: AuditTrailRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = CategoryRepository(db, productRepository, auditTrailRepository, dispatcher)

    @Provides
    fun provideAccountRepository(
        db: FirebaseFirestore,
        auditTrailRepository: AuditTrailRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = AccountRepository(
        db,
        auditTrailRepository,
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
    ) = NotificationTokenRepository(
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
        orderDetailRepository: OrderDetailRepository,
        auditTrailRepository: AuditTrailRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = ProductRepository(
        db,
        productImageRepository,
        productInventoryRepository,
        reviewRepository,
        orderDetailRepository,
        auditTrailRepository,
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
        auditTrailRepository: AuditTrailRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = ProductInventoryRepository(db, auditTrailRepository, dispatcher)

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
        auditTrailRepository: AuditTrailRepository,
        salesRepository: SalesRepository,
        returnRepository: ReturnRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = OrderRepository(
        db,
        orderDetailRepository,
        productRepository,
        auditTrailRepository,
        salesRepository,
        returnRepository,
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
        likeRepository: LikeRepository,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = PostRepository(
        db,
        likeRepository,
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
        notificationTokenRepository: NotificationTokenRepository,
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

    @Provides
    fun provideSalesRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = SalesRepository(
        db,
        dispatcher
    )

    @Provides
    fun provideTermsAndConditionRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = TermsAndConditionRepository(
        db,
        dispatcher
    )

    @Provides
    fun provideSizeChartRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = SizeChartRepository(
        db,
        dispatcher
    )

    @Provides
    fun provideFAQRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = FAQRepository(
        db,
        dispatcher
    )

    @Provides
    fun provideReturnRepository(
        db: FirebaseFirestore,
        @IoDispatcher dispatcher: CoroutineDispatcher
    ) = ReturnRepository(
        db,
        dispatcher
    )
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class IoDispatcher

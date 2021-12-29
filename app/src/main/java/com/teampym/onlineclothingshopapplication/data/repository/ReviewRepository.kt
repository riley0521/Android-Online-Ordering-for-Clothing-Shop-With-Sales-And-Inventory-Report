package com.teampym.onlineclothingshopapplication.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.Review
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.REVIEWS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.presentation.client.reviews.ReviewPagingSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val reviewCollectionRef = db.collection(PRODUCTS_COLLECTION)

    fun getSome(userId: String?, queryReviews: Query) =
        Pager(
            PagingConfig(
                pageSize = 30,
                prefetchDistance = 30,
                enablePlaceholders = false
            )
        ) {
            ReviewPagingSource(
                userId,
                queryReviews
            )
        }

    suspend fun getFive(productId: String): List<Review> {
        return withContext(dispatcher) {
            val reviewList = mutableListOf<Review>()
            val fiveReviewDocuments = reviewCollectionRef
                .document(productId)
                .collection(REVIEWS_SUB_COLLECTION)
                .orderBy("dateReview", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()

            if (fiveReviewDocuments.documents.isNotEmpty()) {
                for (document in fiveReviewDocuments.documents) {
                    val review = document.toObject<Review>()!!.copy(id = document.id, productId = productId)
                    reviewList.add(review)
                }
            }
            reviewList
        }
    }

    suspend fun insert(
        userInformation: UserInformation,
        rate: Double,
        desc: String,
        productId: String,
    ): Review? {
        return withContext(dispatcher) {
            var createdReview = Review(
                userId = userInformation.userId,
                productId = productId,
                userAvatar = userInformation.avatarUrl ?: "",
                username = "${userInformation.firstName} ${userInformation.lastName}",
                rate = rate,
                description = desc,
                dateReview = System.currentTimeMillis()
            )

            try {
                val result = reviewCollectionRef
                    .document(productId)
                    .collection(REVIEWS_SUB_COLLECTION)
                    .add(createdReview)
                    .await()

                return@withContext createdReview.copy(id = result.id)
            } catch (ex: Exception) {
                return@withContext null
            }
        }
    }
}

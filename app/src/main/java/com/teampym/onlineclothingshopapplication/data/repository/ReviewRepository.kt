package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.Review
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.REVIEWS_SUB_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val reviewCollectionRef = db.collection(PRODUCTS_COLLECTION)

    // TODO("Create a paging source.")

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
            var createdReview: Review? = Review(
                userId = userInformation.userId,
                productId = productId,
                userAvatar = userInformation.avatarUrl ?: "",
                username = "${userInformation.firstName} ${userInformation.lastName}",
                rate = rate,
                description = desc,
                dateReview = System.currentTimeMillis()
            )

            createdReview?.let { r ->
                reviewCollectionRef
                    .document(productId)
                    .collection(REVIEWS_SUB_COLLECTION)
                    .add(r)
                    .addOnSuccessListener {
                        createdReview?.id = it.id
                    }.addOnFailureListener {
                        createdReview = null
                        return@addOnFailureListener
                    }
            }
            createdReview
        }
    }
}

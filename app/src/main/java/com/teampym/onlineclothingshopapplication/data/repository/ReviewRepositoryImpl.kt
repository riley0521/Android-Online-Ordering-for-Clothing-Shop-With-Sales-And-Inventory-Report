package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Review
import com.teampym.onlineclothingshopapplication.data.models.UserInformation
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.REVIEWS_SUB_COLLECTION
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    db: FirebaseFirestore
) {

    private val reviewCollectionRef = db.collection(PRODUCTS_COLLECTION)

    suspend fun getFive(productId: String): List<Review> {

        val reviewsQuery = reviewCollectionRef
            .document(productId)
            .collection(REVIEWS_SUB_COLLECTION)
            .orderBy("dateReview", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .await()

        val reviewList = mutableListOf<Review>()
        if (reviewsQuery.documents.isNotEmpty()) {
            for (document in reviewsQuery.documents) {
                val review = document.toObject<Review>()!!.copy(id = document.id, productId = productId)
                reviewList.add(review)
            }
        }
        return reviewList
    }

    suspend fun insert(
        userInformation: UserInformation,
        rate: Double,
        desc: String,
        productId: String,
    ): Review? {
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

        return createdReview
    }
}

package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Review
import com.teampym.onlineclothingshopapplication.data.util.PRODUCTS_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.REVIEWS_SUB_COLLECTION
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ReviewRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) {

    private val reviewCollectionRef = db.collection(PRODUCTS_COLLECTION)

    suspend fun getFive(productId: String): List<Review> {

        val reviewsQuery = reviewCollectionRef
            .document(productId)
            .collection(REVIEWS_SUB_COLLECTION)
            .limit(5)
            .get()
            .await()

        val reviewList = mutableListOf<Review>()
        if(reviewsQuery.documents.isNotEmpty()) {
            for(document in reviewsQuery.documents) {
                val review = document.toObject<Review>()!!.copy(id = document.id, productId = productId)
                reviewList.add(review)
            }
        }
        return reviewList
    }

    suspend fun getAvgRate(productId: String): Double {
        var avgRate = 0.0

        val reviewsQuery = reviewCollectionRef
            .document(productId)
            .collection(REVIEWS_SUB_COLLECTION)
            .get()
            .await()

        if(reviewsQuery.documents.isNotEmpty()) {
            avgRate = reviewsQuery
                .documents.sumOf { it["rate"].toString().toDouble() } / reviewsQuery.documents.size
        }
        return avgRate
    }

}
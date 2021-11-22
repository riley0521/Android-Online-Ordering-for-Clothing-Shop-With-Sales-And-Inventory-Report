package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Like
import com.teampym.onlineclothingshopapplication.data.util.LIKES_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LikeRepositoryImpl @Inject constructor(
    db: FirebaseFirestore
) {

    private val postCollectionRef = db.collection(POSTS_COLLECTION)

    suspend fun getAll(postId: String): List<Like> {
        val likeList = mutableListOf<Like>()

        val fetchLikesQuery = postCollectionRef
            .document(postId)
            .collection(LIKES_SUB_COLLECTION)
            .get()
            .await()

        fetchLikesQuery?.let { querySnapshot ->
            for (doc in querySnapshot.documents) {
                val like = doc.toObject<Like>()!!.copy(id = doc.id, postId = postId)
                likeList.add(like)
            }
        }

        return likeList
    }

    suspend fun add(postId: String, like: Like): Boolean {
        var isSuccessful = true

        postCollectionRef
            .document(postId)
            .collection(LIKES_SUB_COLLECTION)
            .add(like)
            .addOnSuccessListener {
            }.addOnFailureListener {
                isSuccessful = false
                return@addOnFailureListener
            }
        return isSuccessful
    }

    suspend fun remove(postId: String, userId: String): Boolean {
        var isSuccessful = true

        postCollectionRef
            .document(postId)
            .collection(LIKES_SUB_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener {
                it.documents[0].reference.delete().addOnSuccessListener {
                }.addOnFailureListener {
                    isSuccessful = false
                    return@addOnFailureListener
                }
            }.addOnFailureListener {
                isSuccessful = false
                return@addOnFailureListener
            }
        return isSuccessful
    }
}

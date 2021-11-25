package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.Like
import com.teampym.onlineclothingshopapplication.data.util.LIKES_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LikeRepository @Inject constructor(
    db: FirebaseFirestore,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val postCollectionRef = db.collection(POSTS_COLLECTION)

    suspend fun getAll(postId: String): List<Like> {
        return withContext(dispatcher) {
            val likeList = mutableListOf<Like>()

            val likeDocuments = postCollectionRef
                .document(postId)
                .collection(LIKES_SUB_COLLECTION)
                .get()
                .await()

            likeDocuments?.let { querySnapshot ->
                for (doc in querySnapshot.documents) {
                    val like = doc.toObject<Like>()!!.copy(id = doc.id, postId = postId)
                    likeList.add(like)
                }
            }
            likeList
        }
    }

    suspend fun add(postId: String, like: Like): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true
            val result = postCollectionRef
                .document(postId)
                .collection(LIKES_SUB_COLLECTION)
                .add(like)
                .await()
            if (result != null) {
                like.id = result.id
            } else {
                isCompleted = false
            }
            isCompleted
        }
    }

    suspend fun remove(postId: String, userId: String): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true
            postCollectionRef
                .document(postId)
                .collection(LIKES_SUB_COLLECTION)
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener {
                    it.documents[0].reference.delete()
                        .addOnSuccessListener {
                        }.addOnFailureListener {
                            isCompleted = false
                            return@addOnFailureListener
                        }
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
                }
            isCompleted
        }
    }
}

package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Like
import com.teampym.onlineclothingshopapplication.data.util.LIKES_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LikeRepositoryImpl @Inject constructor(
    db: FirebaseFirestore,
    private val postRepository: PostRepositoryImpl
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
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            postCollectionRef
                .document(postId)
                .collection(LIKES_SUB_COLLECTION)
                .add(like)
                .addOnSuccessListener {
                    runBlocking {
                        isCompleted = postRepository.updateLikeCount(postId, 1)
                    }
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
                }
            return@withContext isCompleted
        }
        return isSuccessful
    }

    suspend fun remove(postId: String, userId: String): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
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
                            runBlocking {
                                isCompleted = postRepository.updateLikeCount(postId, -1)
                            }
                        }.addOnFailureListener {
                            isCompleted = false
                            return@addOnFailureListener
                        }
                }.addOnFailureListener {
                    isCompleted = false
                    return@addOnFailureListener
                }
            return@withContext isCompleted
        }
        return isSuccessful
    }
}

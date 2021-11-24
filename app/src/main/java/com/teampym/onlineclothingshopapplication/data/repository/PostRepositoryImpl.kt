package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    db: FirebaseFirestore,
) {

    private val postCollectionRef = db.collection(POSTS_COLLECTION)

    // TODO("Should be changed to paging source")
    fun getAll(): Flow<List<Post>> = callbackFlow {
        val postListener = postCollectionRef
            .addSnapshotListener { value, error ->
                if (error != null) {
                    cancel(message = "Error fetching posts", error)
                    return@addSnapshotListener
                }

                val postList = mutableListOf<Post>()
                value?.let { querySnapshot ->
                    for (doc in querySnapshot.documents) {
                        runBlocking {
                            val post = doc.toObject<Post>()!!.copy(
                                id = doc.id,
                            )
                            postList.add(post)
                        }
                    }
                    offer(postList)
                }
            }
        awaitClose {
            postListener.remove()
        }
    }

    suspend fun insert(post: Post?): Post? {
        val createdPost = withContext(Dispatchers.IO) {
            var createdPostTemp = post
            createdPostTemp?.let { p ->
                postCollectionRef
                    .add(p)
                    .addOnSuccessListener {
                        p.id = it.id
                    }.addOnFailureListener {
                        createdPostTemp = null
                        return@addOnFailureListener
                    }
            }
            return@withContext createdPostTemp
        }
        return createdPost
    }

    suspend fun update(
        postId: String,
        post: Post
    ): Boolean {
        var isSuccessful = true

        val fetchPostQuery = postCollectionRef
            .document(postId)
            .get()
            .await()

        if (fetchPostQuery.data != null) {
            val avatarUrl = post.avatarUrl ?: ""
            val imageUrl = post.imageUrl ?: ""

            val updatePostMap = mapOf<String, Any>(
                "title" to post.title,
                "description" to post.description,
                "createdBy" to post.createdBy,
                "avatarUrl" to avatarUrl,
                "imageUrl" to imageUrl,
                "dateCreated" to System.currentTimeMillis()
            )

            postCollectionRef
                .document(postId)
                .update(updatePostMap)
                .addOnSuccessListener {
                }.addOnFailureListener {
                    isSuccessful = false
                    return@addOnFailureListener
                }
        } else {
            isSuccessful = false
        }
        return isSuccessful
    }

    suspend fun updateLikeCount(postId: String, count: Long): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true

            val postQuery = postCollectionRef
                .document(postId)
                .get()
                .await()

            postQuery?.let { doc ->
                val post = doc.toObject<Post>()!!.copy(id = doc.id)
                val updateLikeCountMap = mapOf<String, Any>(
                    "numberOfLikes" to post.numberOfLikes + count
                )

                doc.reference.set(updateLikeCountMap, SetOptions.merge())
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        isCompleted = false
                        return@addOnFailureListener
                    }
            }
            return@withContext isCompleted
        }
        return isSuccessful
    }

    suspend fun updateCommentCount(postId: String, count: Long): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true

            val postQuery = postCollectionRef
                .document(postId)
                .get()
                .await()

            postQuery?.let { doc ->
                val post = doc.toObject<Post>()!!.copy(id = doc.id)
                val updateLikeCountMap = mapOf<String, Any>(
                    "numberOfComments" to post.numberOfComments + count
                )

                doc.reference.set(updateLikeCountMap, SetOptions.merge())
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        isCompleted = false
                        return@addOnFailureListener
                    }
            }
            return@withContext isCompleted
        }
        return isSuccessful
    }
}

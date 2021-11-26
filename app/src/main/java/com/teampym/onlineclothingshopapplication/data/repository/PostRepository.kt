package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.di.IoDispatcher
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    db: FirebaseFirestore,
    private val notificationTokenRepositoryImpl: NotificationTokenRepositoryImpl,
    @IoDispatcher val dispatcher: CoroutineDispatcher
) {

    private val postCollectionRef = db.collection(POSTS_COLLECTION)

    // TODO("Should be changed to paging source")
    @ExperimentalCoroutinesApi
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
                        val post = doc.toObject<Post>()!!.copy(
                            id = doc.id,
                        )
                        postList.add(post)
                    }
                    offer(postList)
                }
            }
        awaitClose {
            postListener.remove()
        }
    }

    suspend fun insert(post: Post?): Post? {
        return withContext(dispatcher) {
            var createdPostTemp = post
            createdPostTemp?.let { p ->
                p.dateCreated = System.currentTimeMillis()
                postCollectionRef
                    .add(p)
                    .addOnSuccessListener {
                        p.id = it.id
                    }.addOnFailureListener {
                        createdPostTemp = null
                        return@addOnFailureListener
                    }
            }
            createdPostTemp
        }
    }

    suspend fun update(
        postId: String,
        post: Post
    ): Boolean {
        return withContext(dispatcher) {
            var isSuccessful = true

            val postDocument = postCollectionRef
                .document(postId)
                .get()
                .await()

            if (postDocument != null) {
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
                    .set(updatePostMap, SetOptions.merge())
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        isSuccessful = false
                        return@addOnFailureListener
                    }
            } else {
                isSuccessful = false
            }
            isSuccessful
        }
    }

    suspend fun updateLikeCount(postId: String, count: Long): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true

            val postDocument = postCollectionRef
                .document(postId)
                .get()
                .await()

            postDocument?.let { doc ->
                val post = doc.toObject<Post>()!!.copy(id = doc.id)
                post.numberOfLikes += count

                doc.reference.set(post, SetOptions.merge())
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        isCompleted = false
                        return@addOnFailureListener
                    }
            }
            isCompleted
        }
    }

    suspend fun updateCommentCount(postId: String, count: Long): Boolean {
        return withContext(dispatcher) {
            var isCompleted = true

            val postDocument = postCollectionRef
                .document(postId)
                .get()
                .await()

            postDocument?.let { doc ->
                val post = doc.toObject<Post>()!!.copy(id = doc.id)
                post.numberOfComments += count

                doc.reference.set(post, SetOptions.merge())
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        isCompleted = false
                        return@addOnFailureListener
                    }
            }
            isCompleted
        }
    }
}

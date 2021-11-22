package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Post
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    db: FirebaseFirestore,
    private val likeRepository: LikeRepositoryImpl,
    private val commentRepository: CommentRepositoryImpl
) {

    private val postCollectionRef = db.collection(POSTS_COLLECTION)

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
                        CoroutineScope(Dispatchers.IO).launch {
                            // get like and comment list here
                            val likeList = likeRepository.getAll(doc.id)

                            val commentList = commentRepository.getAll(doc.id)

                            val post = doc.toObject<Post>()!!.copy(
                                id = doc.id,
                                likeList = likeList,
                                commentList = commentList
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
        var createdPost: Post? = post

        post?.let { p ->
            postCollectionRef
                .add(p)
                .addOnSuccessListener {
                    createdPost?.id = it.id
                }.addOnFailureListener {
                    createdPost = null
                    return@addOnFailureListener
                }
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
}

package com.teampym.onlineclothingshopapplication.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.toObject
import com.teampym.onlineclothingshopapplication.data.models.Comment
import com.teampym.onlineclothingshopapplication.data.util.COMMENTS_SUB_COLLECTION
import com.teampym.onlineclothingshopapplication.data.util.POSTS_COLLECTION
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
    db: FirebaseFirestore,
    private val postRepository: PostRepositoryImpl
) {

    private val postCollectionRef = db.collection(POSTS_COLLECTION)

    suspend fun getAll(postId: String): List<Comment> {
        val commentList = mutableListOf<Comment>()

        val fetchCommentsQuery = postCollectionRef
            .document(postId)
            .collection(COMMENTS_SUB_COLLECTION)
            .get()
            .await()

        fetchCommentsQuery?.let { querySnapshot ->
            for (doc in querySnapshot.documents) {
                val comment = doc.toObject<Comment>()!!.copy(id = doc.id)
                commentList.add(comment)
            }
        }

        return commentList
    }

    suspend fun insert(postId: String, comment: Comment?): Comment? {
        val createdComment = withContext(Dispatchers.IO) {
            var createdCommentTemp = comment
            createdCommentTemp?.let { c ->
                postCollectionRef
                    .document(postId)
                    .collection(COMMENTS_SUB_COLLECTION)
                    .add(c)
                    .addOnSuccessListener {
                        runBlocking {
                            c.id = it.id
                            if (!postRepository.updateCommentCount(postId, 1)) {
                                createdCommentTemp = null
                            }
                        }
                    }.addOnFailureListener {
                        createdCommentTemp = null
                        return@addOnFailureListener
                    }
            }
            return@withContext createdCommentTemp
        }
        return createdComment
    }

    suspend fun update(postId: String, comment: Comment?): Comment? {
        val updatedComment = withContext(Dispatchers.IO) {
            var updatedCommentTemp = comment
            updatedCommentTemp?.let { c ->
                val updateCommentMap = mapOf<String, Any>(
                    "description" to c.description,
                    "dateCommented" to c.dateCommented
                )

                postCollectionRef
                    .document(postId)
                    .collection(COMMENTS_SUB_COLLECTION)
                    .document(c.id)
                    .set(updateCommentMap, SetOptions.merge())
                    .addOnSuccessListener {
                    }.addOnFailureListener {
                        updatedCommentTemp = null
                        return@addOnFailureListener
                    }
            }
            return@withContext updatedCommentTemp
        }
        return updatedComment
    }

    suspend fun delete(postId: String, commentId: String): Boolean {
        val isSuccessful = withContext(Dispatchers.IO) {
            var isCompleted = true
            postCollectionRef
                .document(postId)
                .collection(COMMENTS_SUB_COLLECTION)
                .document(commentId)
                .delete()
                .addOnSuccessListener {
                    runBlocking {
                        isCompleted = postRepository.updateCommentCount(postId, -1)
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
